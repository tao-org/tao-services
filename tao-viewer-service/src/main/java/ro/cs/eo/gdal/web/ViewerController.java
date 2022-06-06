package ro.cs.eo.gdal.web;

import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import ro.cs.eo.gdal.reader.ImageInfo;
import ro.cs.eo.gdal.reader.info.ProductInfo;
import ro.cs.eo.gdal.service.GDALTileMapService;
import ro.cs.tao.eodata.EOProduct;
import ro.cs.tao.persistence.PersistenceManager;
import ro.cs.tao.services.commons.ControllerBase;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
public class ViewerController extends ControllerBase {

    private final AnnotationConfigApplicationContext context;
    @Autowired
    private PersistenceManager persistenceManager;

    public ViewerController() {
        this.context = new AnnotationConfigApplicationContext();
        this.context.scan("ro.cs.eo.gdal.service");
        this.context.refresh();
    }

    @RequestMapping(value = "/products/view/tile/", method = RequestMethod.GET)
    public ResponseEntity<byte[]> viewTileData(@RequestParam("name") String name,
                                               @RequestParam("z") byte z,
                                               @RequestParam("x") int x,
                                               @RequestParam("y") int y) {
        GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);
        try {
            List<EOProduct> products = persistenceManager.rasterData().getProductsByNames(name);
            if (products == null || products.size() == 0) {
                throw new FileNotFoundException("Product not found.");
            }
            String path = products.get(0).getLocation();
            ProductInfo productInfo = ProductInfo.getProductInfoInstance(name, path);
            byte[] data = gdalTileMapService.getGDALTile(productInfo, z, x, y);
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE);
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IndexOutOfBoundsException e) {
            return new ResponseEntity<>(("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try again.</h3></body></html>").getBytes(), headers, HttpStatus.NOT_FOUND);
        } catch (FileNotFoundException e) {
            return new ResponseEntity<>(("<html><body><h1>" + e.getMessage() + "</h1></body></html>").getBytes(), headers, HttpStatus.NOT_FOUND);
        } catch(IllegalArgumentException e){
            return new ResponseEntity<>(("<html><body><h1>" + e.getMessage() + "</h1><br/><h3>Try another product.</h3></body></html>").getBytes(), headers, HttpStatus.NOT_ACCEPTABLE);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(("<html><body><h1>500</h1><br/><h3>Internal server error.</h3></body></html>").getBytes(), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(value = "/products/view/info/", method = RequestMethod.GET)
    public ResponseEntity<String> viewTileInfo(@RequestParam("name") String name) {
        GDALTileMapService gdalTileMapService = (GDALTileMapService) context.getBean("TileMap");
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE);

        try {
            List<EOProduct> products = persistenceManager.rasterData().getProductsByNames(name);
            if (products == null || products.size() == 0) {
                throw new FileNotFoundException("Product not found.");
            }
            String path = products.get(0).getLocation();
            ProductInfo productInfo = ProductInfo.getProductInfoInstance(name, path);
            ImageInfo imageInfo = gdalTileMapService.getGDALImageInfo(productInfo);
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

    @RequestMapping(value = "/products/view/map/", method = RequestMethod.GET)
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

    @RequestMapping(value = "/products/view/config/set-cache-dir/", method = RequestMethod.POST)
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
}