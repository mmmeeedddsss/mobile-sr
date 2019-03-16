import random

import numpy as np
import tensorflow as tf

from data import get_discriminator_training_set, get_discriminator_test_set


NUM_EPOCHS = 5
BATCH_SIZE = 64

# time to try out keras.layers which is to replace tf.layers in TF 2.0!
def get_discriminator_model():
    model = tf.keras.Sequential([
        tf.keras.layers.Conv2D(32, 5, 1, 'same', activation=tf.nn.leaky_relu),
        tf.keras.layers.MaxPool2D(2, padding='same'),
        tf.keras.layers.Conv2D(64, 3, 1, 'same', activation=tf.nn.leaky_relu),
        tf.keras.layers.MaxPool2D(2, padding='same'),
        tf.keras.layers.Flatten(),
        tf.keras.layers.Dense(98, activation=tf.nn.leaky_relu),
        tf.keras.layers.Dense(2, activation=tf.nn.sigmoid)
    ])
    adam = tf.keras.optimizers.Adam(lr=0.01)
    model.compile(optimizer=adam,
                  loss='binary_crossentropy',
                  metrics=['accuracy'])
    return model

if __name__ == '__main__':
    # set constant seeds
    random.seed(490)
    np.random.seed(491)
    tf.set_random_seed(492)

    discr = get_discriminator_model()
    train_images, train_labels = get_discriminator_training_set(BATCH_SIZE)
    N = train_images.shape[0]

    discr.fit(train_images, train_labels, epochs=5, batch_size=BATCH_SIZE)

    # evaluate test accuracy
    test_images, test_labels = get_discriminator_test_set()
    loss, acc = discr.evaluate(test_images, test_labels)
    print(f'Test accuracy: {acc}')
