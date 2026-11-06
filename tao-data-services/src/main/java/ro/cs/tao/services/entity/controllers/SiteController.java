package ro.cs.tao.services.entity.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ro.cs.tao.Sort;
import ro.cs.tao.SortDirection;
import ro.cs.tao.eodata.Polygon2D;
import ro.cs.tao.gadm.AdminL1Unit;
import ro.cs.tao.gadm.AdminL2Unit;
import ro.cs.tao.gadm.Country;
import ro.cs.tao.serialization.GeometryAdapter;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.services.interfaces.SiteService;
import ro.cs.tao.utils.StringUtilities;
import ro.cs.tao.workspaces.Site;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/site")
@Tag(name = "Sites", description = "Operations related to sites management")
public class SiteController extends DataEntityController<Site, String, SiteService> {

    /**
     * Returns a (paged) list of sites.
     * The optional parameters are either all given or none is set.
     *
     * @param pageNumber    (optional) The page number
     * @param pageSize      (optional) Items per page
     * @param sortByField   (optional) The sort field
     * @param sortDirection (optional) The sort direction (ASC or DESC)
     */
    @Override
    public ResponseEntity<ServiceResponse<?>> list(@RequestParam(name = "pageNumber", required = false) Optional<Integer> pageNumber,
                                                   @RequestParam(name = "pageSize", required = false) Optional<Integer> pageSize,
                                                   @RequestParam(name = "sortBy", required = false) Optional<String> sortByField,
                                                   @RequestParam(name = "sortDirection", required = false) Optional<SortDirection> sortDirection) {
        if (!(pageNumber.isPresent() || pageSize.isPresent() || sortByField.isPresent() || sortDirection.isPresent())) {
            try {
                return prepareResult(this.service.getByUser(currentOwner()));
            } catch (Exception e) {
                return handleException(e);
            }
        } else {
            return prepareResult(this.service.list(pageNumber, pageSize, Sort.by(sortByField.orElse("id"), sortDirection.orElse(SortDirection.ASC))));
        }
    }

    @RequestMapping(value = { "/countries", "/countries/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listCountries() {
        try {
            List<Country> countries = this.service.getCountries();
            if (countries == null || countries.isEmpty()) {
                countries = downloadCountries();
                this.service.saveCountries(countries);
            }
            return prepareResult(countryPayload(countries));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = { "/adml1", "/adml1/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listCountryAdministrativeUnits(@RequestParam(name = "country") String country) {
        try {
            final List<AdminL1Unit> units = countryAdministrativeUnits(country);
            return prepareResult(adminL1Payload(units));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = { "/adml1/union", "/adml1/union/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> unionCountryAdministrativeUnits(@RequestParam(name = "country") String country,
                                                                              @RequestParam(name = "units") List<String> unitIds) {
        try {
            if (unitIds == null || unitIds.isEmpty()) {
                throw new IllegalArgumentException("No administrative units selected");
            }
            final Set<String> selectedUnitIds = new HashSet<>(unitIds);
            final List<AdminL1Unit> units = countryAdministrativeUnits(country);
            Geometry union = null;
            for (AdminL1Unit unit : units) {
                if (unit != null && selectedUnitIds.contains(unit.getId()) && unit.getExtent() != null) {
                    union = union == null ? unit.getExtent() : union.union(unit.getExtent());
                }
            }
            if (union == null) {
                throw new IllegalArgumentException("No matching administrative units found");
            }
            return prepareResult(new GeometryAdapter().unmarshal(union));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = { "/adml2", "/adml2/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> listLevel2CountryAdministrativeUnits(@RequestParam(name = "country") String country,
                                                                                   @RequestParam(name = "unit") String adminL1UnitId) {
        try {
            final List<AdminL2Unit> units = level2AdministrativeUnits(country, adminL1UnitId);
            return prepareResult(adminL2Payload(units));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    @RequestMapping(value = { "/adml2/union", "/adml2/union/" }, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ServiceResponse<?>> unionLevel2CountryAdministrativeUnits(@RequestParam(name = "country") String country,
                                                                                   @RequestParam(name = "unit") String adminL1UnitId,
                                                                                   @RequestParam(name = "units") List<String> unitIds) {
        try {
            if (unitIds == null || unitIds.isEmpty()) {
                throw new IllegalArgumentException("No administrative units selected");
            }
            final Set<String> selectedUnitIds = new HashSet<>(unitIds);
            final List<AdminL2Unit> units = level2AdministrativeUnits(country, adminL1UnitId);
            Geometry union = null;
            for (AdminL2Unit unit : units) {
                if (unit != null && selectedUnitIds.contains(unit.getId()) && unit.getExtent() != null) {
                    union = union == null ? unit.getExtent() : union.union(unit.getExtent());
                }
            }
            if (union == null) {
                throw new IllegalArgumentException("No matching administrative units found");
            }
            return prepareResult(new GeometryAdapter().unmarshal(union));
        } catch (Exception e) {
            return handleException(e);
        }
    }

    /**
     * Returns the definition of a site.
     * Only an administrator or the owner of the site can retrieve the information.
     *
     * @param id    The site identifier
     */
    @Override
    public ResponseEntity<ServiceResponse<?>> get(@PathVariable("id") String id) {
        try {
            final Site entity = this.service.findById(id);
            if (entity == null || !(entity.getUserId().equals(currentUser()) || entity.getUserId().equals(currentOwner()))) {
                throw new IllegalArgumentException("You cannot access this object");
            }
            return prepareResult(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
    /**
     * Creates a new site definition.
     *
     * @param entity    The site definition structure
     */
    @Override
    public ResponseEntity<ServiceResponse<?>> save(@RequestBody Site entity) {
        try {
            entity.setUserId(currentOwner());
            return super.save(entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }
    /**
     * Updates an existing site definition.
     *
     * @param id    The site identifier
     * @param entity    The site definition structure
     */
    @Override
    public ResponseEntity<ServiceResponse<?>> update(@PathVariable("id") String id, @RequestBody Site entity) {
        try {
            if (StringUtilities.isNullOrEmpty(entity.getUserId()) ||
                    !(entity.getUserId().equals(currentUser()) || entity.getUserId().equals(currentOwner()))) {
                throw new IllegalArgumentException("Cannot update this object");
            }
            return super.update(id, entity);
        } catch (Exception e) {
            return handleException(e);
        }
    }

    private List<Country> downloadCountries() {
        final String url = "https://hub.huwise.com/api/explore/v2.1/catalog/datasets/world-administrative-boundaries-countries/exports/geojson/?lang=en";
        final List<Country> countries = new ArrayList<>();
        try (InputStream inputStream = URI.create(url).toURL().openStream()) {
            final JsonNode root = new ObjectMapper().readTree(inputStream);
            final JsonNode features = root != null ? root.get("features") : null;
            if (features != null && features.isArray()) {
                final GeometryAdapter geometryAdapter = new GeometryAdapter();
                for (JsonNode feature : features) {
                    final JsonNode properties = feature.get("properties");
                    final Polygon2D polygon = polygonFromGeometry(feature.get("geometry"));
                    final String iso3Code = textValue(properties, "iso3_code");
                    if (polygon != null && !StringUtilities.isNullOrEmpty(iso3Code)) {
                        final Geometry geometry = singlePolygon(geometryAdapter.marshal(polygon.toWKT()));
                        final Country country = new Country();
                        country.setIso3Code(iso3Code);
                        country.setName(textValue(properties, "preferred_term"));
                        country.setLongitude(geometry.getCentroid().getX());
                        country.setLatitude(geometry.getCentroid().getY());
                        country.setExtent(geometry);
                        country.setEnabled(true);
                        countries.add(country);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Cannot retrieve countries");
        }
        return countries;
    }

    private List<Map<String, String>> countryPayload(List<Country> countries) {
        final List<Map<String, String>> payload = new ArrayList<>();
        if (countries != null) {
            final GeometryAdapter geometryAdapter = new GeometryAdapter();
            for (Country country : countries) {
                final Map<String, String> item = new LinkedHashMap<>();
                item.put("iso3Code", country.getIso3Code());
                item.put("name", country.getName());
                item.put("extent", geometryAdapter.unmarshal(country.getExtent()));
                payload.add(item);
            }
        }
        return payload;
    }

    private List<Map<String, String>> adminL1Payload(List<AdminL1Unit> units) {
        final List<Map<String, String>> payload = new ArrayList<>();
        if (units != null) {
            final GeometryAdapter geometryAdapter = new GeometryAdapter();
            for (AdminL1Unit unit : units) {
                final Map<String, String> item = new LinkedHashMap<>();
                item.put("id", unit.getId());
                item.put("name", unit.getName());
                item.put("countryCode", unit.getCountryCode());
                item.put("extent", geometryAdapter.unmarshal(unit.getExtent()));
                payload.add(item);
            }
        }
        return payload;
    }

    private List<Map<String, String>> adminL2Payload(List<AdminL2Unit> units) {
        final List<Map<String, String>> payload = new ArrayList<>();
        if (units != null) {
            final GeometryAdapter geometryAdapter = new GeometryAdapter();
            for (AdminL2Unit unit : units) {
                final Map<String, String> item = new LinkedHashMap<>();
                item.put("id", unit.getId());
                item.put("name", unit.getName());
                item.put("adminL1Id", unit.getAdminL1Id());
                item.put("extent", geometryAdapter.unmarshal(unit.getExtent()));
                payload.add(item);
            }
        }
        return payload;
    }

    private List<AdminL1Unit> countryAdministrativeUnits(String countryCodeIso3) {
        List<AdminL1Unit> units = this.service.getAdminL1Units(countryCodeIso3);
        if (units == null || units.isEmpty()) {
            units = downloadAdminL1(countryCodeIso3);
            this.service.updateCountry(countryCodeIso3, units);
        }
        return units;
    }

    private List<AdminL2Unit> level2AdministrativeUnits(String countryCodeIso3, String adminL1UnitId) {
        List<AdminL2Unit> units = this.service.getAdminL2Units(adminL1UnitId);
        if (units == null || units.isEmpty()) {
            final List<AdminL1Unit> level1Units = countryAdministrativeUnits(countryCodeIso3);
            final List<AdminL2Unit> downloadedUnits = downloadAdminL2(countryCodeIso3, level1Units);
            this.service.saveAdminL2Units(downloadedUnits);
            units = filterByAdminL1Id(downloadedUnits, adminL1UnitId);
        }
        return units;
    }

    private List<AdminL1Unit> downloadAdminL1(String countryCodeIso3) {
        final String url = "https://github.com/wmgeolab/geoBoundaries/raw/9469f09/releaseData/gbOpen/" + countryCodeIso3 + "/ADM1/geoBoundaries-" + countryCodeIso3 + "-ADM1_simplified.geojson";
        final List<AdminL1Unit> result = new ArrayList<>();
        try (InputStream inputStream = URI.create(url).toURL().openStream()) {
            final JsonNode root = new ObjectMapper().readTree(inputStream);
            final JsonNode features = root != null ? root.get("features") : null;
            if (features != null && features.isArray()) {
                for (JsonNode feature : features) {
                    final JsonNode properties = feature.get("properties");
                    final Polygon2D polygon = polygonFromGeometry(feature.get("geometry"));
                    if (polygon != null) {
                        final AdminL1Unit unit = new AdminL1Unit();
                        unit.setId(textValue(properties, "shapeID"));
                        unit.setName(textValue(properties, "shapeName"));
                        unit.setCountryCode(countryCodeIso3);
                        unit.setExtent(singlePolygon(new GeometryAdapter().marshal(polygon.toWKT())));
                        result.add(unit);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Cannot retrieve administrative units for country " + countryCodeIso3);
        }
        return result;
    }

    private List<AdminL2Unit> downloadAdminL2(String countryCodeIso3, List<AdminL1Unit> level1Units) {
        final String url = "https://github.com/wmgeolab/geoBoundaries/raw/9469f09/releaseData/gbOpen/" + countryCodeIso3 + "/ADM2/geoBoundaries-" + countryCodeIso3 + "-ADM2_simplified.geojson";
        final List<AdminL2Unit> result = new ArrayList<>();
        try (InputStream inputStream = URI.create(url).toURL().openStream()) {
            final JsonNode root = new ObjectMapper().readTree(inputStream);
            final JsonNode features = root != null ? root.get("features") : null;
            if (features != null && features.isArray()) {
                final GeometryAdapter geometryAdapter = new GeometryAdapter();
                for (JsonNode feature : features) {
                    final JsonNode properties = feature.get("properties");
                    final Polygon2D polygon = polygonFromGeometry(feature.get("geometry"));
                    if (polygon != null) {
                        final Geometry extent = singlePolygon(geometryAdapter.marshal(polygon.toWKT()));
                        final String adminL1Id = adminL1IdFor(extent, level1Units);
                        if (!StringUtilities.isNullOrEmpty(adminL1Id)) {
                            final AdminL2Unit unit = new AdminL2Unit();
                            unit.setId(textValue(properties, "shapeID"));
                            unit.setName(textValue(properties, "shapeName"));
                            unit.setAdminL1Id(adminL1Id);
                            unit.setExtent(extent);
                            result.add(unit);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Cannot retrieve level 2 administrative units for country " + countryCodeIso3);
        }
        return result;
    }

    private List<AdminL2Unit> filterByAdminL1Id(List<AdminL2Unit> units, String adminL1UnitId) {
        final List<AdminL2Unit> result = new ArrayList<>();
        if (units != null) {
            for (AdminL2Unit unit : units) {
                if (unit != null && adminL1UnitId.equals(unit.getAdminL1Id())) {
                    result.add(unit);
                }
            }
        }
        return result;
    }

    private String adminL1IdFor(Geometry geometry, List<AdminL1Unit> level1Units) {
        if (geometry == null || level1Units == null || level1Units.isEmpty()) {
            return null;
        }
        String bestUnitId = null;
        double bestArea = 0;
        for (AdminL1Unit unit : level1Units) {
            final Geometry level1Extent = unit != null ? unit.getExtent() : null;
            if (level1Extent != null && level1Extent.intersects(geometry)) {
                try {
                    final double intersectionArea = level1Extent.intersection(geometry).getArea();
                    if (intersectionArea > bestArea) {
                        bestArea = intersectionArea;
                        bestUnitId = unit.getId();
                    }
                } catch (Exception ignored) {
                    if (level1Extent.covers(geometry.getCentroid())) {
                        return unit.getId();
                    }
                }
            }
        }
        if (bestUnitId == null) {
            for (AdminL1Unit unit : level1Units) {
                final Geometry level1Extent = unit != null ? unit.getExtent() : null;
                if (level1Extent != null && level1Extent.covers(geometry.getCentroid())) {
                    bestUnitId = unit.getId();
                    break;
                }
            }
        }
        return bestUnitId;
    }

    private Polygon2D polygonFromGeometry(JsonNode geometry) {
        if (geometry == null) {
            return null;
        }
        final JsonNode coordinates = geometry.get("coordinates");
        if (coordinates == null || !coordinates.isArray()) {
            return null;
        }
        final Polygon2D polygon = new Polygon2D();
        final String type = textValue(geometry, "type");
        if ("MultiPolygon".equalsIgnoreCase(type)) {
            int index = 0;
            for (JsonNode currentPolygon : coordinates) {
                if (appendExteriorRing(polygon, currentPolygon, index)) {
                    index++;
                }
            }
        } else {
            appendExteriorRing(polygon, coordinates, 0);
        }
        return polygon.getNumPoints() > 0 ? polygon : null;
    }

    private Geometry singlePolygon(Geometry geometry) {
        if (!(geometry instanceof MultiPolygon)) {
            return geometry;
        }
        try {
            final Geometry union = geometry.union();
            return union instanceof Polygon ? union : union.convexHull();
        } catch (Exception e) {
            logger.warning("Cannot convert geometry to Polygon: " + e.getMessage());
            return geometry.convexHull();
        }
    }

    private boolean appendExteriorRing(Polygon2D polygon, JsonNode polygonCoordinates, int index) {
        if (polygonCoordinates == null || !polygonCoordinates.isArray() || polygonCoordinates.isEmpty()) {
            return false;
        }
        final JsonNode exteriorRing = polygonCoordinates.get(0);
        if (exteriorRing == null || !exteriorRing.isArray() || exteriorRing.isEmpty()) {
            return false;
        }
        double firstX = 0;
        double firstY = 0;
        double previousX = 0;
        double previousY = 0;
        boolean hasPoint = false;
        for (JsonNode point : exteriorRing) {
            if (point != null && point.isArray() && point.size() >= 2) {
                final double x = point.get(0).asDouble();
                final double y = point.get(1).asDouble();
                if (!hasPoint) {
                    firstX = x;
                    firstY = y;
                    hasPoint = true;
                }
                previousX = x;
                previousY = y;
                polygon.append(index, x, y);
            }
        }
        if (hasPoint && (Double.compare(firstX, previousX) != 0 || Double.compare(firstY, previousY) != 0)) {
            polygon.append(index, firstX, firstY);
        }
        return hasPoint;
    }

    private String textValue(JsonNode node, String fieldName) {
        if (node == null) {
            return null;
        }
        final JsonNode value = node.get(fieldName);
        return value != null && !value.isNull() ? value.asText() : null;
    }
}
