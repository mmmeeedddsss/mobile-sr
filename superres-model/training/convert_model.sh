output_tflite=$1

printf "*****\nFreezing Graph\n*****\n"
freeze_graph --input_saved_model_dir saved-model \
    --output_node_names output_image \
    --output_graph frozen.pb

printf "*****\nOptimizing Frozen Graph\n*****\n"
python -m tensorflow.python.tools.optimize_for_inference \
    --input frozen.pb \
    --output optim.pb \
    --input_names input_image \
    --output_names output_image \
    --frozen_graph

printf "*****\nConverting Optimized Graph to TFLite\n*****\n"
toco --graph_def_file optim.pb \
    --output_file $output_tflite \
    --output_format TFLITE \
    --inference-type FLOAT \
    --inference_input_type FLOAT \
    --input_arrays input_image \
     --input_shapes 1,256,256,3 \
     --output_arrays output_image
