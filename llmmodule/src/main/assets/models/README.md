# Local LLM Models Directory

This directory contains ExecuTorch-converted models for offline LLM inference.

## Required Files

1. **Model File**: `llama-7b-4bit-q8.pte`
   - ExecuTorch-converted LLaMA 7B model with 4-bit quantization
   - Size: ~500MB (after quantization)

2. **Tokenizer**: `tokenizer.model`
   - SentencePiece tokenizer for Korean/English text processing
   - Size: ~2MB

3. **Configuration**: `model_config.json`
   - Model configuration including vocabulary size, context length, etc.
   - LoRA adapter settings (if applicable)

## Asset Preparation

1. Convert PyTorch model to ExecuTorch format:
   ```python
   from torch._export import capture_pre_autograd_graph
   from executorch import to_edge

   # Load and export model
   model = load_llama_model()
   example_inputs = (prompt_tensor,)
   exported = capture_pre_autograd_graph(model, example_inputs)

   # Edge compilation with quantization
   edge_program = to_edge(
       exported,
       compile_config={"quant_config": "q8"}  # 8-bit quantization
   )

   # Save to .pte format
   edge_program.write("llama-7b-4bit-q8.pte")
   ```

2. Copy files to this directory:
   ```bash
   cp llama-7b-4bit-q8.pte /path/to/llmmodule/src/main/assets/models/
   cp tokenizer.model /path/to/llmmodule/src/main/assets/models/
   ```

## Memory Requirements

- **Minimum**: 2GB free RAM for LLaMA 7B 4-bit
- **Recommended**: 3GB+ free RAM for optimal performance
- **KV Cache**: Additional 200-500MB for long contexts

## Performance Notes

- **First Load**: 2-5 seconds for model initialization
- **Token Generation**: 20-50 tokens/second on modern devices
- **Memory Usage**: ~1.2GB after loading (including cache)
- **Thread Count**: Use device CPU cores for parallelization

## Troubleshooting

### Asset Loading Issues
- Ensure files are not compressed by build system
- Check file permissions in assets directory
- Verify file sizes match expected values

### Out of Memory
- Reduce max context length
- Use smaller model (LLaMA 2B instead of 7B)
- Lower quantization precision (Q4 instead of Q8)

### Slow Performance
- Increase thread count: `module.setNumThreads(availableProcessors())`
- Enable KV caching for sequential generation
- Use GPU delegate if available (future enhancement)