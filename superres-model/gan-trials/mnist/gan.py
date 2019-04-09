import random
import os

import numpy as np
import tensorflow as tf
from PIL import Image

from data import *
from schedulers import GoodfellowScheduler

LATENT_SIZE = 100
IMG_DIR = 'images'
PRINT_EVERY = 25
PRETRAIN_DISCR = False
NUM_EPOCHS = 20 
BATCH_SIZE = 64

def save_imbatch(imdata, batch_count, imdir):
    batch_dir = os.path.join(imdir, str(batch_count))
    os.makedirs(batch_dir, exist_ok=True)
    j = 0
    for float_img in imdata:
        img = np.squeeze(((float_img + 0.5) * 255).astype('uint8'))
        save_path = os.path.join(batch_dir, f'{j}.png')
        img_obj = Image.fromarray(img)
        img_obj.save(save_path, 'PNG')
        j += 1

# time to try out keras.layers which is to replace tf.layers in TF 2.0!
def build_discriminator_model():
    # define the model using the Keras functional API
    inputs = tf.keras.layers.Input(shape=(28, 28, 1))
    x = tf.keras.layers.Conv2D(32, 5, 1, 'same', activation=tf.nn.leaky_relu)(inputs)
    x = tf.keras.layers.MaxPool2D(2, padding='same')(x)
    x = tf.keras.layers.Conv2D(64, 3, 1, 'same', activation=tf.nn.leaky_relu)(x)
    x = tf.keras.layers.MaxPool2D(2, padding='same')(x)
    x = tf.keras.layers.Flatten()(x)
    # x = tf.keras.layers.Dense(98, activation=tf.nn.leaky_relu)(x)
    predictions = tf.keras.layers.Dense(1, activation=tf.nn.sigmoid)(x)
    # create the model and compile it
    model = tf.keras.Model(inputs=inputs, outputs=predictions)
    return model

def build_generator_model(latent_size):
    # once again, use the Keras functional API
    inputs = tf.keras.layers.Input(shape=(latent_size,))
    x = tf.keras.layers.Dense(7 * 7 * 256, activation=tf.nn.leaky_relu)(inputs) # dense layer 
    x = tf.keras.layers.Reshape((7, 7, 256))(x) # reshape to 3D
    # upsample to 14x14x256
    x = tf.keras.layers.Conv2DTranspose(128, 3, 2, 'same', activation=tf.nn.leaky_relu)(x)
    # upsample to 28x28x1
    output = tf.keras.layers.Conv2DTranspose(1, 3, 2, 'same', activation=tf.nn.leaky_relu)(x)
    # create the model, no need to compile yet
    model = tf.keras.Model(inputs=inputs, outputs=output)
    return model

if __name__ == '__main__':
    # set constant seeds
    random.seed(490)
    np.random.seed(491)
    tf.set_random_seed(492)
    
    # get the discriminator model and compile it
    print('DISCRIMINATOR:')
    discriminator = build_discriminator_model()
    discriminator.compile(
        optimizer=tf.keras.optimizers.RMSprop(lr=0.0005),
        loss='binary_crossentropy',
        metrics=['accuracy'])
    discriminator.summary()
    
    # get the generator
    print('GENERATOR:')
    generator = build_generator_model(LATENT_SIZE)
    generator.summary()

    # create a gan by combining both
    noise_input = tf.keras.layers.Input(shape=(LATENT_SIZE,)) # input noise
    fake_img = generator(noise_input) # pass it through the generator
    discriminator.trainable = False # make the discriminator untrainable for the GAN
    discr_fake_pred = discriminator(fake_img) # pass the fake img through the discriminator
    combined = tf.keras.Model(inputs=noise_input, outputs=discr_fake_pred) # create a combined model
    print('COMBINED:')
    combined.compile(
        optimizer=tf.keras.optimizers.RMSprop(lr=0.002),
        loss=['binary_crossentropy'])
    combined.summary()

    # pretrain the discriminator with mnist & noise if requested
    if PRETRAIN_DISCR:
        # load the discriminator sets
        train_images, train_labels = get_discriminator_training_set(BATCH_SIZE)
        N = train_images.shape[0]
        test_images, test_labels = get_discriminator_test_set()
        # pre-train the discriminator
        discriminator.fit(train_images, train_labels, epochs=NUM_EPOCHS, batch_size=BATCH_SIZE)
        # evaluate test accuracy
        loss, acc = discriminator.evaluate(test_images, test_labels)
        print(f'Discriminator Test accuracy: {acc}')
    
    # load the MNIST sets
    train_images, _ = get_training_set()
    N = train_images.shape[0]
    test_images, _ = get_test_set()

    scheduler = GoodfellowScheduler(1) # train D k times, G once
    hb_size = BATCH_SIZE // 2
    adv_loss, discr_loss = np.NaN, np.NaN 
    step = 0
    saved_batch_count = 0
    discr_losses = []
    adv_losses = []
    try:
        for ep in range(NUM_EPOCHS):
            print('************************')
            print('************************')
            print(f'Epoch {ep}/{NUM_EPOCHS}')
            i = 0
            while i < N:
                if scheduler.train_discriminator():
                    # create half a noise batch
                    noise_hbatch = get_noise_batch(hb_size, LATENT_SIZE)
                    # pass it through the generator to create a fake half-batch
                    fake_hbatch = generator.predict_on_batch(noise_hbatch)
                    fake_hlabels = np.zeros(hb_size)
                    # get a real mnist data half-batch
                    mnist_hbatch = train_images[i:i+hb_size, :, :, :]
                    mnist_hlabels = np.ones(hb_size)
                    # combine them and train the discriminator
                    discr_batch = np.concatenate((mnist_hbatch, fake_hbatch))
                    discr_labels = np.concatenate((mnist_hlabels, fake_hlabels))
                    # train the discriminator
                    discr_loss, discr_acc = discriminator.train_on_batch(discr_batch, discr_labels)
                    # increment the batch counter
                    i += hb_size 
                else:
                    # train the generator with a noise batch
                    noise_batch = get_noise_batch(BATCH_SIZE, LATENT_SIZE)
                    noise_labels = np.ones(BATCH_SIZE) # fake as real to train the generator
                    adv_loss = combined.train_on_batch(noise_batch, noise_labels)
               
                if step % PRINT_EVERY == 0:
                    print('-------------------------------------')
                    print(f'Half-batch {i//hb_size}/{N//hb_size}')
                    print(f'Discriminator loss: {discr_loss}')
                    print(f'Discriminator accuracy: {discr_acc:.3f}')
                    print(f'Adversarial loss: {adv_loss}')
                    discr_losses.append(discr_loss)
                    adv_losses.append(adv_loss)

                step += 1
            # save generator outputs after every epoch
            save_imbatch(fake_hbatch, saved_batch_count, IMG_DIR)
            saved_batch_count += 1
    except KeyboardInterrupt: # if training is interrupted, still save losses
        pass
    # save the loss history to an npz file
    np.savez('losses.npz', discr_loss=np.array(discr_losses), adv_loss=np.array(adv_losses))
