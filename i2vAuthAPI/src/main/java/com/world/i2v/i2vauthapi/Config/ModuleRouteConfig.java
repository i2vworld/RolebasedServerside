package com.world.i2v.i2vauthapi.Config;

import com.world.i2v.i2vauthapi.entity.Module;
import com.world.i2v.i2vauthapi.repository.ModuleRepository;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ModuleRouteConfig {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]");
    private static final Pattern MULTI_UNDERSCORE = Pattern.compile("_+");
    private static final Pattern LEADING_TRAILING_UNDERSCORE = Pattern.compile("^_|_$");
    private static final String API_PREFIX = "/api/";

    private final ModuleRepository moduleRepository;

    // Volatile snapshots: reads always see a complete, immutable-in-practice map set.
    // Rebuilds happen off to the side and are published with a single swap, so
    // concurrent permission checks never observe an empty or half-loaded registry.
    private volatile Map<String, ModuleRouteInfo> routesByCode = Collections.emptyMap();
    private volatile Set<String> allActiveRoutes = Collections.emptySet();
    private volatile Map<String, List<String>> functionalGroups = Collections.emptyMap();
    private volatile Map<Long, ModuleRouteInfo> routesById = Collections.emptyMap();
    private volatile Map<String, ModuleRouteInfo> routesByPath = Collections.emptyMap();

    public ModuleRouteConfig(ModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    @PostConstruct
    public void loadRoutesFromDatabase() {
        log.info("=== LOADING MODULE ROUTES FROM DATABASE ===");
        rebuild(moduleRepository.findAll());
    }

    public void refreshRoutes() {
        log.info("Manual refresh of module routes triggered");
        rebuild(moduleRepository.findAll());
    }

    /**
     * Builds a fresh snapshot off to the side and publishes it atomically.
     */
    private synchronized void rebuild(List<Module> modules) {
        Map<String, ModuleRouteInfo> codeMap = new ConcurrentHashMap<>();
        Set<String> activeRoutes = ConcurrentHashMap.newKeySet();
        Map<String, List<String>> groups = new ConcurrentHashMap<>();
        Map<Long, ModuleRouteInfo> idMap = new ConcurrentHashMap<>();
        Map<String, ModuleRouteInfo> pathMap = new ConcurrentHashMap<>();

        int activeCount = 0;

        for (Module module : modules) {
            if (Boolean.TRUE.equals(module.getIsActive())
                    && registerModule(module, codeMap, activeRoutes, groups, idMap, pathMap)) {
                activeCount++;
            }
        }

        this.routesByCode = codeMap;
        this.allActiveRoutes = activeRoutes;
        this.functionalGroups = groups;
        this.routesById = idMap;
        this.routesByPath = pathMap;

        log.info("Loaded {} active module routes", activeRoutes.size());
        log.info("Module codes: {}", codeMap.keySet());
    }

    private boolean registerModule(Module module,
                                   Map<String, ModuleRouteInfo> codeMap,
                                   Set<String> activeRoutes,
                                   Map<String, List<String>> groups,
                                   Map<Long, ModuleRouteInfo> idMap,
                                   Map<String, ModuleRouteInfo> pathMap) {
        String code = module.getCode();
        String name = module.getName();
        String routePath = normalizeRoutePath(module.getRoutePath());

        if (routePath == null || routePath.isEmpty()) {
            log.warn("Module '{}' has no route path, skipping", name);
            return false;
        }

        Set<String> alternativeRoutes = generateAlternativeRoutes(routePath, name, code);

        ModuleRouteInfo info = ModuleRouteInfo.builder()
                .moduleId(module.getId())
                .moduleCode(code)
                .moduleName(name)
                .primaryRoute(routePath)
                .alternativeRoutes(alternativeRoutes)
                .displayOrder(module.getDisplayOrder() != null ? module.getDisplayOrder() : 999)
                .icon(module.getIcon())
                .requiresPremium(module.getRequiresPremium() != null ? module.getRequiresPremium() : false)
                .isVisible(module.getIsVisible() != null ? module.getIsVisible() : true)
                .parentModuleId(module.getParentModule() != null ? module.getParentModule().getId() : null)
                .build();

        if (code != null && !code.isEmpty()) {
            codeMap.put(code.toUpperCase(), info);
            codeMap.put(code.toLowerCase(), info);
        }

        String normalizedName = normalizeModuleName(name);
        codeMap.put(normalizedName, info);
        codeMap.put(normalizedName.toUpperCase(), info);

        idMap.put(module.getId(), info);

        // Reverse index: every known spelling of the route resolves to the same info in O(1).
        pathMap.put(routePath, info);
        for (String alternative : alternativeRoutes) {
            pathMap.putIfAbsent(alternative, info);
        }

        activeRoutes.add(routePath);
        activeRoutes.addAll(alternativeRoutes);

        String functionalGroup = extractFunctionalGroup(routePath);
        groups.computeIfAbsent(functionalGroup, k -> new ArrayList<>()).add(routePath);

        return true;
    }

    private String normalizeRoutePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        String normalized = path.trim().toLowerCase();

        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    private Set<String> generateAlternativeRoutes(String primaryRoute, String moduleName, String moduleCode) {
        Set<String> alternatives = new HashSet<>();

        if (primaryRoute.endsWith("s")) {
            alternatives.add(primaryRoute.substring(0, primaryRoute.length() - 1));
        } else {
            alternatives.add(primaryRoute + "s");
        }

        if (primaryRoute.contains("-")) {
            alternatives.add(primaryRoute.replace("-", "_"));
        }
        if (primaryRoute.contains("_")) {
            alternatives.add(primaryRoute.replace("_", "-"));
        }

        String normalizedName = normalizeModuleName(moduleName);
        alternatives.add("/" + normalizedName);
        alternatives.add("/" + normalizedName + "s");

        if (moduleCode != null && !moduleCode.isEmpty()) {
            String normalizedCode = normalizeModuleName(moduleCode);
            alternatives.add("/" + normalizedCode);
            alternatives.add("/" + normalizedCode + "s");
        }

        alternatives.remove(primaryRoute);

        return alternatives;
    }

    private String normalizeModuleName(String input) {
        if (input == null) return "";

        String normalized = NON_ALNUM.matcher(input.trim().toLowerCase()).replaceAll("_");
        normalized = MULTI_UNDERSCORE.matcher(normalized).replaceAll("_");
        return LEADING_TRAILING_UNDERSCORE.matcher(normalized).replaceAll("");
    }

    private String extractFunctionalGroup(String routePath) {
        if (routePath == null) return "unknown";

        String path = routePath.startsWith(API_PREFIX)
                ? routePath.substring(API_PREFIX.length())
                : routePath;

        while (path.startsWith("/")) {
            path = path.substring(1);
        }

        int slash = path.indexOf('/');
        return slash == -1 ? path : path.substring(0, slash);
    }

    public Optional<ModuleRouteInfo> getRouteInfo(String moduleIdentifier) {
        if (moduleIdentifier == null || moduleIdentifier.trim().isEmpty()) {
            return Optional.empty();
        }

        Map<String, ModuleRouteInfo> snapshot = routesByCode;
        String key = moduleIdentifier.trim();

        ModuleRouteInfo info = snapshot.get(key);
        if (info != null) return Optional.of(info);

        info = snapshot.get(key.toUpperCase());
        if (info != null) return Optional.of(info);

        info = snapshot.get(key.toLowerCase());
        if (info != null) return Optional.of(info);

        info = snapshot.get(normalizeModuleName(key));
        if (info != null) return Optional.of(info);

        try {
            Long id = Long.parseLong(key);
            return Optional.ofNullable(routesById.get(id));
        } catch (NumberFormatException e) {
            // Not an ID
        }

        return Optional.empty();
    }

    public Optional<ModuleRouteInfo> getRouteInfoById(Long moduleId) {
        return Optional.ofNullable(routesById.get(moduleId));
    }

    public List<String> getRoutesByGroup(String groupName) {
        if (groupName == null) return Collections.emptyList();
        return functionalGroups.getOrDefault(groupName.toLowerCase(), Collections.emptyList());
    }

    public boolean isValidRoute(String routePath) {
        String normalized = normalizeRoutePath(routePath);
        return normalized != null && allActiveRoutes.contains(normalized);
    }

    public Optional<ModuleRouteInfo> findModuleByRoute(String routePath) {
        String normalized = normalizeRoutePath(routePath);
        if (normalized == null) return Optional.empty();
        return Optional.ofNullable(routesByPath.get(normalized));
    }

    public Set<String> getAllActiveRoutes() {
        return Collections.unmodifiableSet(allActiveRoutes);
    }

    public Set<String> getAllModuleCodes() {
        Set<String> codes = new HashSet<>();
        for (ModuleRouteInfo info : routesById.values()) {
            if (info.getModuleCode() != null) {
                codes.add(info.getModuleCode());
            }
        }
        return Collections.unmodifiableSet(codes);
    }

    public Set<String> getAllFunctionalGroups() {
        return Collections.unmodifiableSet(functionalGroups.keySet());
    }

    public List<ModuleRouteInfo> getAllRouteInfos() {
        return new ArrayList<>(routesById.values());
    }

    public Map<String, ModuleRouteInfo> getRoutesByCode() {
        return Collections.unmodifiableMap(routesByCode);
    }

    public Map<String, List<String>> getFunctionalGroups() {
        return Collections.unmodifiableMap(functionalGroups);
    }

    @Data
    @Builder
    public static class ModuleRouteInfo {
        private Long moduleId;
        private String moduleCode;
        private String moduleName;
        private String primaryRoute;
        private Set<String> alternativeRoutes;
        private Integer displayOrder;
        private String icon;
        private Boolean requiresPremium;
        private Boolean isVisible;
        private Long parentModuleId;

        public String[] getAllRoutes() {
            Set<String> all = new HashSet<>();
            all.add(primaryRoute);
            if (alternativeRoutes != null) {
                all.addAll(alternativeRoutes);
            }
            return all.toArray(new String[0]);
        }

        public boolean matches(String routePath) {
            if (routePath == null) return false;

            String normalized = routePath.trim().toLowerCase();
            if (!normalized.startsWith("/")) {
                normalized = "/" + normalized;
            }
            if (normalized.length() > 1 && normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            if (primaryRoute.equals(normalized)) {
                return true;
            }

            return alternativeRoutes != null && alternativeRoutes.contains(normalized);
        }
    }
}
