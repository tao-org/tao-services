package ro.cs.eo.gdal.web;

import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.cs.eo.gdal.reader.ImageInfo;
import ro.cs.eo.gdal.reader.TileReader;
import ro.cs.eo.gdal.reader.info.FormatDescriptor;
import ro.cs.eo.gdal.reader.info.ProductDescriptor;
import ro.cs.eo.gdal.reader.info.ProductDescriptorFactory;
import ro.cs.eo.gdal.service.GDALTileMapService;
import ro.cs.tao.persistence.RepositoryProvider;
import ro.cs.tao.services.commons.ControllerBase;
import ro.cs.tao.services.commons.ResponseStatus;
import ro.cs.tao.services.commons.ServiceResponse;
import ro.cs.tao.workspaces.Repository;
import ro.cs.tao.workspaces.RepositoryType;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/view")
public class ViewerController extends ControllerBase {

    private final AnnotationConfigApplicationContext context;

    @Autowired
    private RepositoryProvider repositoryProvider;

    public ViewerController() {
        this.context = new AnnotationConfigApplicationContext();
        this.context.scan("ro.cs.eo.gdal.service");
        this.context.refresh();
    }

    @RequestMapping(value = "/scale", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> getScale(@RequestParam("file") String relativePath) {
        final Repository localRepository = getLocalRepository();
        final String path = localRepository.resolve(relativePath);
        return prepareResult(TileReader.getScale(Paths.get(path)));
    }

    @RequestMapping(value = "/scale", method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<ServiceResponse<?>> changeScale(@RequestParam("file") String relativePath,
                                                          @RequestParam("min") double min,
                                                          @RequestParam("max") double max) {
        final Repository localRepository = getLocalRepository();
        final String path = localRepository.resolve(relativePath);
        TileReader.setScale(Paths.get(path), new double[] { min, max });
        return prepareResult("New scale set", ResponseStatus.SUCCEEDED);
    }

    @RequestMapping(value = "/tile", method = RequestMethod.GET)
    public ResponseEntity<byte[]> viewTileData(@RequestParam("file") String relativePath,
                                               @RequestParam("z") byte z,
                                               @RequestParam("x") int x,
                                               @RequestParam("y") int y) {
        GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
        HttpHeaders headers = new HttpHeaders();
        try {
            final Repository localRepository = getLocalRepository();
            final String path = localRepository.resolve(relativePath);
            ProductDescriptor productDescriptor = ProductDescriptorFactory.getDescriptor(Paths.get(path));
            double[] scale = TileReader.getScale(Paths.get(path));
            if (scale != null && scale.length == 2) {
                FormatDescriptor formatDescriptor = productDescriptor.getFormatDescriptor();
                formatDescriptor.setPixelMinScale((int) scale[0]);
                formatDescriptor.setPixelMaxScale((int) scale[1]);
                //formatDescriptor.setNoDataValue(override.getKeyThree());
                formatDescriptor.setPriority(1);
            }
            byte[] data = gdalTileMapService.getGDALTile(productDescriptor, z, x, y);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE);
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=60");
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IndexOutOfBoundsException e) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
            return new ResponseEntity<>(("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try again.</h3></body></html>").getBytes(), headers, HttpStatus.NOT_FOUND);
        } catch (FileNotFoundException e) {
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
            return new ResponseEntity<>(("<html><body><h1>" + e.getMessage() + "</h1></body></html>").getBytes(), headers, HttpStatus.NOT_FOUND);
        } catch(IllegalArgumentException e){
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
            return new ResponseEntity<>(("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try another product.</h3></body></html>").getBytes(), headers, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            e.printStackTrace();
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
            return new ResponseEntity<>(("<html><body><h1>500</h1><br/><h3>Internal server error.</h3></body></html>").getBytes(), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "info", method = RequestMethod.GET)
    public ResponseEntity<String> viewTileInfo(@RequestParam("relativePath") String relativePath) {
        GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);

        try {
            /*List<EOProduct> products = persistenceManager.rasterData().getProductsByNames(name);
            if (products == null || products.size() == 0) {
                throw new FileNotFoundException("Product not found.");
            }
            String path = products.get(0).getLocation();*/
            final Repository localRepository = getLocalRepository();
            final String path = localRepository.resolve(relativePath);
            final String name = relativePath.substring(relativePath.lastIndexOf('/') + 1);
            ProductDescriptor productDescriptor = ProductDescriptorFactory.getDescriptor(Paths.get(path));
            double[] scale = TileReader.getScale(Paths.get(path));
            if (scale != null && scale.length == 2) {
                FormatDescriptor formatDescriptor = productDescriptor.getFormatDescriptor();
                formatDescriptor.setPixelMinScale((int) scale[0]);
                formatDescriptor.setPixelMaxScale((int) scale[1]);
                //formatDescriptor.setNoDataValue(override.getKeyThree());
                formatDescriptor.setPriority(1);
            }
            ImageInfo imageInfo = gdalTileMapService.getGDALImageInfo(productDescriptor);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            return new ResponseEntity<>(imageInfo.toJson(), headers, HttpStatus.OK);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>("<html><body><h1>" + e.getMessage() + "</h1></body></html>", headers, HttpStatus.NOT_FOUND);
        } catch (IndexOutOfBoundsException e) {
            return new ResponseEntity<>("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try again.</h3></body></html>", headers, HttpStatus.NOT_FOUND);
        } catch(IllegalArgumentException e){
            return new ResponseEntity<>("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try another product.</h3></body></html>", headers, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("<html><body><h1>500</h1><br/><h3>Internal server error.</h3></body></html>", headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "map", method = RequestMethod.GET)
    public ResponseEntity<byte[]> viewMap() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        try {
            byte[] data = IOUtils.toByteArray(ViewerController.class.getResourceAsStream("view1.html"));
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("<html><body><h1>500</h1><br/><h3>Internal server error.</h3></body></html>".getBytes(), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "config", method = RequestMethod.POST)
    public ResponseEntity<String> setCacheDir(@RequestParam("cache-dir") String newCacheDir) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        try {
            GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
            Path cacheDirpath = Paths.get(newCacheDir);
            if (Files.exists(cacheDirpath)) {
                gdalTileMapService.setCacheDirPath(cacheDirpath);
                return new ResponseEntity<>("<html><body><h1>OK</h1><br/><h3>Cache directory path updated successfully.</h3></body></html>", headers, HttpStatus.OK);
            }
            return new ResponseEntity<>("<html><body><h1>BAD</h1><br/><h3>Cache directory path not updated. Invalid path.</h3></body></html>", headers, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("<html><body><h1>500</h1><br/><h3>Internal server error.</h3></body></html>", headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Repository getLocalRepository() {
        return repositoryProvider.getByUser(currentUser()).stream().filter(w -> w.getType() == RepositoryType.LOCAL).findFirst().get();
    }
}