package com.senior_project.group_1.mobilesr;
import android.graphics.Bitmap;



import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.util.ArrayList;
import java.util.LinkedList;

import static com.ibm.icu.impl.Assert.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static com.senior_project.group_1.mobilesr.ImageProcessingTask.chunkImages;


@RunWith(RobolectricTestRunner.class)
public class DivideImageUnitTest {

    /*The variables that are used in testing the unit*/
    private static Bitmap testBitmap = null;
    private static int[] colors = null;
    private static int[] actual = null;
    private static int[] expected = null;
    private static LinkedList<int[]> patchColors = null;
    private static ArrayList<Bitmap> expectedAL = null;

    /*Arrangements that is made before all tests are run*/
    @BeforeClass
    public static void setup() {
        colors = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        testBitmap = Bitmap.createBitmap(colors, 4,4, Bitmap.Config.ARGB_8888);
        actual = new int[2*2];
        expected = new int[2*2];


    }

    /*Initializations that is made before each tests*/
    @Before
    public void init() {
        patchColors = null;
        expectedAL = null;
        chunkImages = null;
        expectedAL = new ArrayList<>();
        patchColors = new LinkedList<>();
        patchColors.add(new int[] {0, 1, 4, 5});
        patchColors.add(new int[] {2, 3, 6, 7});
        patchColors.add(new int[] {8, 9, 12, 13});
        patchColors.add(new int[] {10, 11, 14, 15});

    }

    /*This test divides the image with zero overlap*/
    @Test
    public void testDivideImageWithZeroOverlap() {
        for(int[] color : patchColors)
            expectedAL.add(Bitmap.createBitmap(color, 2, 2, Bitmap.Config.ARGB_8888));

        ImageProcessingTask.divideImage(testBitmap, 2,2,0,0);
        for(int i=0; i<chunkImages.size(); i++)
                chunkImages.get(i).setPixels(patchColors.get(i), 0, 2, 0, 0, 2,2);

        for(int i=0; i<chunkImages.size(); i++) {
            expectedAL.get(i).getPixels(expected, 0, 2, 0,0, 2,2);
            chunkImages.get(i).getPixels(actual, 0, 2, 0,0, 2, 2);
            assertArrayEquals(expected, actual);
        }


    }

    /*(Image.getWidth()-overlapX*2) % (chunkWidth - overlapX*2) != 0 case fails for divide image*/
    @Test
    public void throwsRunTimeExceptionWhenImageSizesIsNotDivisibleOverlapX() {
        try {
            ImageProcessingTask.divideImage(testBitmap, 2,2,1,0);
            fail("Runtime Exception did not happen!");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("/ by zero"));
        }

    }

    /*(Image.getHeight()-overlapY*2) % (chunkHeight - overlapY*2) != 0 case fails for divide image*/
    @Test
    public void throwsRunTimeExceptionWhenImageSizesIsNotDivisibleOverlapY() {
        try {
            ImageProcessingTask.divideImage(testBitmap, 2,2,0,1);
            fail("Runtime Exception did not happen!");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("/ by zero"));
        }

    }

    /*Overlap from x axis greater than chunk width case*/
    @Test
    public void shouldThrowRunTimeExceptionWhenOverlapXGreaterThanChunkWidth() {
        try {
            ImageProcessingTask.divideImage(testBitmap, 2,2,3,1);
            fail("Runtime Exception did not happen!");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Overlap value greater than chunk sizes"));
        }
    }

    /*Overlap from y axis greater than chunk height case*/
    @Test
    public void shouldThrowRunTimeExceptionWhenOverlapYGreaterThanChunkHeight() {
        try {
            ImageProcessingTask.divideImage(testBitmap, 2,2,3,1);
            fail("Runtime Exception did not happen!");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Overlap value greater than chunk sizes"));
        }
    }


}

