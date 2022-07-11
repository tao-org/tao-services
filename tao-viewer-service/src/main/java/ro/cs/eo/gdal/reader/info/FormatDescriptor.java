package ro.cs.eo.gdal.reader.info;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FormatDescriptor {
    private List<String> namePatterns;
    private String subfolderPattern;
    private List<Band> bands;
    private Integer pixelMinScale;
    private Integer pixelMaxScale;
    private Integer noDataValue;
    private int priority;

    private List<Pattern> namePatternsCompiled;

    public List<String> getNamePatterns() {
        return namePatterns;
    }

    public void setNamePatterns(List<String> namePatterns) {
        this.namePatterns = namePatterns;
    }

    public String getSubfolderPattern() {
        return subfolderPattern;
    }

    public void setSubfolderPattern(String subfolderPattern) {
        this.subfolderPattern = subfolderPattern;
    }

    public List<Band> getBands() {
        return bands;
    }

    public void setBands(List<Band> bands) {
        this.bands = bands;
    }

    public Integer getPixelMinScale() {
        return pixelMinScale;
    }

    public void setPixelMinScale(Integer pixelMinScale) {
        this.pixelMinScale = pixelMinScale;
    }

    public Integer getPixelMaxScale() {
        return pixelMaxScale;
    }

    public void setPixelMaxScale(Integer pixelMaxScale) {
        this.pixelMaxScale = pixelMaxScale;
    }

    public Integer getNoDataValue() {
        return noDataValue;
    }

    public void setNoDataValue(Integer noDataValue) {
        this.noDataValue = noDataValue;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean matches(String expression) {
        if (this.namePatterns != null) {
            if (this.namePatternsCompiled == null) {
                this.namePatternsCompiled = this.namePatterns.stream().map(Pattern::compile).collect(Collectors.toList());
            }
            return this.namePatternsCompiled.stream().anyMatch(p -> p.matcher(expression).find());
        }
        return false;
    }

    public boolean multiRaster() {
        return this.bands != null && this.bands.stream().map(Band::getPattern).distinct().count() > 1;
    }

    public static class Band {
        private int indexInRaster;
        private String pattern;

        public int getIndexInRaster() {
            return indexInRaster;
        }

        public void setIndexInRaster(int indexInRaster) {
            this.indexInRaster = indexInRaster;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public boolean matches(String expression) {
            return this.pattern != null && Pattern.compile(this.pattern).matcher(expression).find();
        }
    }
}
