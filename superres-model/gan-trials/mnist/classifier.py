import random

import tensorflow as tf
import numpy as np

import idx


# time to try out keras.layers which is to replace tf.layers in TF 2.0!
def get_classifier_model():
    model = tf.keras.Sequential([
        tf.keras.layers.Conv2D(32, 5, 1, 'same', activation=tf.nn.leaky_relu),
        tf.keras.layers.MaxPool2D(2, padding='same'),
        tf.keras.layers.Conv2D(64, 3, 1, 'same', activation=tf.nn.leaky_relu),
        tf.keras.layers.MaxPool2D(2, padding='same'),
        tf.keras.layers.Flatten(),
        tf.keras.layers.Dense(1024, activation=tf.nn.leaky_relu),
        tf.keras.layers.Dense(10, activation=tf.nn.softmax)
    ])
    adam = tf.keras.optimizers.Adam(lr=0.001)
    model.compile(optimizer=adam,
                  loss='sparse_categorical_crossentropy',
                  metrics=['accuracy'])
    return model

def preprocess_set(data):
    # data is nhwc
    return data.astype('float32') / 255.0 - 0.5

def get_training_set():
    train_images = preprocess_set(np.expand_dims(
        idx.readfile('dataset/train-images-idx3-ubyte', 3),
        axis=-1))
    train_labels = np.expand_dims(
        idx.readfile('dataset/train-labels-idx1-ubyte', 1),
        axis=-1)
    return train_images, train_labels

def get_test_set():
    test_images = preprocess_set(np.expand_dims(
        idx.readfile('dataset/t10k-images-idx3-ubyte', 3),
        axis=-1))
    test_labels = np.expand_dims(
        idx.readfile('dataset/t10k-labels-idx1-ubyte', 1),
        axis=-1)
    return test_images, test_labels

if __name__ == '__main__':
    # set constant seeds
    random.seed(490)
    np.random.seed(491)
    tf.set_random_seed(492)

    model = get_classifier_model()
    train_images, train_labels = get_training_set()

    # train the model
    model.fit(train_images, train_labels, epochs=5, batch_size=32)

    # evaluate test accuracy
    test_images, test_labels = get_test_set()
    loss, acc = model.evaluate(test_images, test_labels)
    print(f'Test accuracy: {acc}')
