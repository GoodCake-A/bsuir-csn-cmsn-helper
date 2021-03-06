package main.filter.impl;

import main.filter.FilterStrategy;
import main.filter.ImageType;

import java.util.Arrays;
import java.util.List;

public class LowPassFilter implements FilterStrategy {
    private List<ImageType> typesOfImages = Arrays.asList(ImageType.GRAY, ImageType.RGB);
    private int[][] matrix;
    private int divider;
    private final static int RGB_SIZE = 3;

    public static final LowPassFilter FILTER_A = new LowPassFilter(9, new int[][]{{1, 1, 1}, {1, 1, 1}, {1, 1, 1}});
    public static final LowPassFilter FILTER_B = new LowPassFilter(10, new int[][]{{1, 1, 1}, {1, 2, 1}, {1, 1, 1}});
    public static final LowPassFilter FILTER_C = new LowPassFilter(16, new int[][]{{1, 2, 1}, {2, 4, 2}, {1, 2, 1}});

    public LowPassFilter(int divider, int[][] matrix) {
        if (divider < 1) {
            divider = 1;
        }

        if (matrix == null || matrix.length != 3 || matrix[0].length != 3 || matrix[1].length != 3 || matrix[2].length != 3) {
            throw new IllegalArgumentException("Filter matrix must be 3x3");
        }

        this.divider = divider;
        this.matrix = matrix;
    }

    @Override
    public byte[] doFilter(byte[] imageData, int height, int width, ImageType imageType) {
        byte[] filteredImageData = null;

        switch (imageType) {
            case RGB:
                if (imageData.length != width * height * RGB_SIZE) {
                    throw new IllegalArgumentException("Size of imageData should be equal width * height");
                }

                filteredImageData = filterRgbImage(imageData, width, height);
                break;
            case GRAY:
                if (imageData.length != width * height) {
                    throw new IllegalArgumentException("Size of imageData should be equal width * height");
                }

                filteredImageData = filterGrayscaleImage(imageData, width, height);
                break;
            default:
                throw new IllegalArgumentException("Image type " + imageType.name() + " does not support");
        }

        return filteredImageData;
    }

    @Override
    public boolean compatibleWith(ImageType imageType) {
        return typesOfImages.contains(imageType);
    }

    private byte[] resizeGrayscaleImage(byte[] primaryImage, int imageWidth, int imageHeight) {
        byte[] resizedImage = new byte[(imageHeight + 2) * (imageWidth + 2)];

        for (int i = 0, n = 0; i < imageHeight; i++, n++) {
            for (int j = 0, m = 0; j < imageWidth; j++, m++) {
                resizedImage[n * (imageWidth + 2) + m] = primaryImage[i * imageWidth + j];

                if (j == 0 || j == imageWidth - 1) {
                    m++;
                    resizedImage[n * (imageWidth + 2) + m] = primaryImage[i * imageWidth + j];
                }
            }

            if (n == 0 || n == imageHeight) {
                i--;
            }
        }

        return resizedImage;
    }

    private byte[] resizeRgbImage(byte[] primaryImage, int imageWidth, int imageHeight) {
        byte[] resizedImage = new byte[(imageHeight + 2) * (imageWidth + 2) * RGB_SIZE];

        for (int i = 0, n = 0; i < imageHeight; i++, n++) {
            for (int j = 0, m = 0; j < imageWidth; j++, m++) {
                int resizedIndex = (n * (imageWidth + 2) + m) * RGB_SIZE;
                int primaryIndex = (i * imageWidth + j) * RGB_SIZE;

                resizedImage[resizedIndex] = primaryImage[primaryIndex];
                resizedImage[resizedIndex + 1] = primaryImage[primaryIndex + 1];
                resizedImage[resizedIndex + 2] = primaryImage[primaryIndex + 2];

                if (j == 0 || j == imageWidth - 1) {
                    m++;
                    resizedIndex += RGB_SIZE;
                    resizedImage[resizedIndex] = primaryImage[primaryIndex];
                    resizedImage[resizedIndex + 1] = primaryImage[primaryIndex + 1];
                    resizedImage[resizedIndex + 2] = primaryImage[primaryIndex + 2];
                }
            }

            if (n == 0 || n == imageHeight) {
                i--;
            }
        }

        return resizedImage;
    }

    private byte[] filterGrayscaleImage(byte[] primaryImage, int imageWidth, int imageHeight) {
        byte[] resizedImage = resizeGrayscaleImage(primaryImage, imageWidth, imageHeight);
        byte[] filteredImage = new byte[imageHeight * imageWidth];

        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {
                int sum = 0;

                sum += matrix[0][0] * Byte.toUnsignedInt(resizedImage[i * (imageWidth + 2) + j - 1 + imageWidth + 2 + 1]);
                sum += matrix[0][1] * Byte.toUnsignedInt(resizedImage[i * (imageWidth + 2) + j + imageWidth + 2 + 1]);
                sum += matrix[0][2] * Byte.toUnsignedInt(resizedImage[i * (imageWidth + 2) + j + 1 + imageWidth + 2 + 1]);

                sum += matrix[1][0] * Byte.toUnsignedInt(resizedImage[(i + 1) * (imageWidth + 2) + j - 1 + imageWidth + 2 + 1]);
                sum += matrix[1][1] * Byte.toUnsignedInt(resizedImage[(i + 1) * (imageWidth + 2) + j + imageWidth + 2 + 1]);
                sum += matrix[1][2] * Byte.toUnsignedInt(resizedImage[(i + 1) * (imageWidth + 2) + j + 1 + imageWidth + 2 + 1]);

                sum += matrix[2][0] * Byte.toUnsignedInt(resizedImage[(i - 1) * (imageWidth + 2) + j - 1 + imageWidth + 2 + 1]);
                sum += matrix[2][1] * Byte.toUnsignedInt(resizedImage[(i - 1) * (imageWidth + 2) + j + imageWidth + 2 + 1]);
                sum += matrix[2][2] * Byte.toUnsignedInt(resizedImage[(i - 1) * (imageWidth + 2) + j + 1 + imageWidth + 2 + 1]);

                sum = sum / divider;

                if (sum > 255) {
                    sum = 255;
                } else if (sum < 0) {
                    sum = 0;
                }

                filteredImage[i * imageWidth + j] = (byte) sum;
            }
        }

        return filteredImage;
    }

    private byte[] filterRgbImage(byte[] primaryImage, int imageWidth, int imageHeight) {
        byte[] resizedImage = resizeRgbImage(primaryImage, imageWidth, imageHeight);
        byte[] filteredImage = new byte[imageHeight * imageWidth * 3];

        for (int i = 0; i < imageHeight; i++) {
            for (int j = 0; j < imageWidth; j++) {
                for (int k = 0; k < 3; k++) {
                    int sum = 0;

                    sum += matrix[0][0] * Byte.toUnsignedInt(resizedImage[(i * (imageWidth + 2) + j - 1) * 3 + k + (imageWidth + 2 + 1) * 3]);
                    sum += matrix[0][1] * Byte.toUnsignedInt(resizedImage[(i * (imageWidth + 2) + j) * 3 + k + (imageWidth + 2 + 1) * 3]);
                    sum += matrix[0][2] * Byte.toUnsignedInt(resizedImage[(i * (imageWidth + 2) + j + 1) * 3 + k + (imageWidth + 2 + 1) * 3]);

                    sum += matrix[1][0] * Byte.toUnsignedInt(resizedImage[((i + 1) * (imageWidth + 2) + j - 1) * 3 + k + (imageWidth + 2 + 1) * 3]);
                    sum += matrix[1][1] * Byte.toUnsignedInt(resizedImage[((i + 1) * (imageWidth + 2) + j) * 3 + k + (imageWidth + 2 + 1) * 3]);
                    sum += matrix[1][2] * Byte.toUnsignedInt(resizedImage[((i + 1) * (imageWidth + 2) + j + 1) * 3 + k + (imageWidth + 2 + 1) * 3]);

                    sum += matrix[2][0] * Byte.toUnsignedInt(resizedImage[((i - 1) * (imageWidth + 2) + j - 1) * 3 + k + (imageWidth + 2 + 1) * 3]);
                    sum += matrix[2][1] * Byte.toUnsignedInt(resizedImage[((i - 1) * (imageWidth + 2) + j) * 3 + k + (imageWidth + 2 + 1) * 3]);
                    sum += matrix[2][2] * Byte.toUnsignedInt(resizedImage[((i - 1) * (imageWidth + 2) + j + 1) * 3 + k + (imageWidth + 2 + 1) * 3]);

                    sum = sum / divider;

                    if (sum > 255) {
                        sum = 255;
                    } else if (sum < 0) {
                        sum = 0;
                    }

                    filteredImage[(i * imageWidth + j) * 3 + k] = (byte) sum;
                }
            }
        }

        return filteredImage;
    }
}
