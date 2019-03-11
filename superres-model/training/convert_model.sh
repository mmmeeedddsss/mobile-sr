output_tflite=$1
input_tensor="model/resized_image"
output_tensor="output_image"


printf "*****\nFreezing Graph\n*****\n"
freeze_graph --input_saved_model_dir saved-model \
    --output_node_names $output_tensor \
    --output_graph frozen.pb

printf "*****\nOptimizing Frozen Graph\n*****\n"
python -m tensorflow.python.tools.optimize_for_inference \
    --input frozen.pb \
    --output optim.pb \
    --input_names $input_tensor \
    --output_names $output_tensor \
    --frozen_graph

printf "*****\nConverting Optimized Graph to TFLite\n*****\n"
toco --graph_def_file optim.pb \
    --output_file $output_tflite \
    --output_format TFLITE \
    --inference-type FLOAT \
    --inference_input_type FLOAT \
    --input_arrays $input_tensor \
    --input_shapes 1,128,128,3 \
    --output_arrays $output_tensor
