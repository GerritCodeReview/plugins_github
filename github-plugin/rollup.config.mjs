import terser from '@rollup/plugin-terser';
import { nodeResolve } from '@rollup/plugin-node-resolve';

export default {
  input: 'target/web/src/main/ts/main.js',
  treeshake: false,
  output: {
    format: 'iife',
    compact: true,
    file: 'target/classes/static/github-plugin.js',
  },
  context: 'window',
  plugins: [
    terser(),
    nodeResolve(),
  ],
}