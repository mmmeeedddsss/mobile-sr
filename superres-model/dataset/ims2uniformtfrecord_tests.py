import unittest

import numpy as np

from ims2npy import divide_image

class TestDivideImageBasic(unittest.TestCase):
    def setUp(self):
        # create a 4x4x1 image
        a = np.array([[0, 1, 2, 3],
                      [4, 5, 6, 7],
                      [8, 9, 10, 11],
                      [12, 13, 14, 15]])
        self.a = np.expand_dims(a, axis=2)

    def test_1x1(self):
        # try dividing into 1x1 patches
        tested = divide_image(self.a, (1, 1))
        actual = np.arange(16).reshape(16, 1, 1, 1)
        self.assertTrue(np.all(tested == actual))

    def test_2x2(self):
        # try to divide into 2x2 patches
        tested = divide_image(self.a, (2, 2))
        actual = np.expand_dims(
            np.array([[[0, 1], [4, 5]],
                      [[2, 3], [6, 7]],
                      [[8, 9], [12, 13]],
                      [[10, 11], [14, 15]]]),
            axis=3)
        self.assertTrue(np.all(tested == actual))

    def test_3x3(self):
        # try to divide into 3x3 patches
        tested = divide_image(self.a, (3, 3))
        actual = np.expand_dims(
            np.array([[[0, 1, 2],
                       [4, 5, 6],
                       [8, 9, 10]]]),
            axis=3)
        self.assertTrue(np.all(tested == actual))

    def test_4x4(self):
        # try to divide into 4x4 patches
        tested = divide_image(self.a, (4, 4))
        actual = np.expand_dims(
            np.array([[[0, 1, 2, 3],
                       [4, 5, 6, 7],
                       [8, 9, 10, 11],
                       [12, 13, 14, 15]]]),
            axis=3)
        self.assertTrue(np.all(tested == actual))

    def test_1x3(self):
        tested = divide_image(self.a, (1, 3))
        actual = np.expand_dims(
            np.array([[[0, 1, 2]],
                      [[4, 5, 6]],
                      [[8, 9, 10]],
                      [[12, 13, 14]]]),
            axis=3)
        self.assertTrue(np.all(tested == actual))

if __name__ == '__main__':
    unittest.main()
