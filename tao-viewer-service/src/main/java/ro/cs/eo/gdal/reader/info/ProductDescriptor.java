package ro.cs.eo.gdal.reader.info;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ProductDescriptor {
    private final FormatDescriptor formatDescriptor;
    private final Path productPath;
    private Path[] bandPaths;

    public ProductDescriptor(FormatDescriptor formatDescriptor, Path productPath) throws IOException {
        this.formatDescriptor = formatDescriptor;
        this.productPath = productPath;
        initialize();
    }

    public FormatDescriptor getFormatDescriptor() {
        return formatDescriptor;
    }

    public Path getProductPath() {
        return productPath;
    }

    public Path[] getBandPaths() {
        return bandPaths;
    }

    public Integer getPixelMinScale() {
        return formatDescriptor.getPixelMinScale();
    }

    public Integer getPixelMaxScale() {
        return formatDescriptor.getPixelMaxScale();
    }

    public Integer getNoDataValue() {
        return formatDescriptor.getNoDataValue();
    }

    private void initialize() throws IOException {
        List<FormatDescriptor.Band> expectedBands = this.formatDescriptor.getBands();
        if (this.formatDescriptor.multiRaster()) {
            try (Stream<Path> stream = Files.walk(productPath, FileVisitOption.FOLLOW_LINKS)) {
                String subfolderPattern = this.formatDescriptor.getSubfolderPattern();
                List<Path> files;
                if (subfolderPattern != null) {
                    Path path = stream.filter(f -> Pattern.compile(subfolderPattern).matcher(f.getFileName().toString()).matches()).findFirst().orElse(null);
                    if (path == null) {
                        throw new IOException("Expected subfolder not found");
                    }
                    try (Stream<Path> substream = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
                        files = substream.collect(Collectors.toList());
                    }
                } else {
                    files = stream.collect(Collectors.toList());
                }
                this.bandPaths = new Path[expectedBands.size()];
                int idx = 0;
                for (FormatDescriptor.Band band : expectedBands) {
                    Path bandFile = files.stream().filter(f -> band.matches(f.toString())).findFirst().orElse(null);
                    if (bandFile == null) {
                        throw new IOException("Expected band not found");
                    }
                    this.bandPaths[idx++] = bandFile;
                }
            }
        } else if (Files.isDirectory(this.productPath)) {
            FormatDescriptor.Band band = expectedBands.get(0);
            String subfolderPattern = this.formatDescriptor.getSubfolderPattern();
            try (Stream<Path> stream = Files.walk(productPath, FileVisitOption.FOLLOW_LINKS)) {
                if (subfolderPattern != null) {
                    Path path = stream.filter(f -> Pattern.compile(subfolderPattern).matcher(f.getFileName().toString()).matches()).findFirst().orElse(null);
                    if (path == null) {
                        throw new IOException("Expected subfolder not found");
                    }
                    try (Stream<Path> substream = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
                        Path bandFile = substream.filter(f -> band.matches(f.getFileName().toString())).findFirst().orElse(null);
                        if (bandFile == null) {
                            throw new IOException("Expected band not found");
                        }
                    }
                } else {
                    Path bandFile = stream.filter(f -> band.matches(f.getFileName().toString())).findFirst().orElse(null);
                    if (bandFile == null) {
                        throw new IOException("Expected band not found");
                    }
                    this.bandPaths = new Path[] { bandFile };
                }
            }
        } else {
            this.bandPaths = new Path[] { this.productPath };
        }
    }
}
