#version 100
// GlowPlay film post-processing shader.
//
// Applies, in a single pass: unsharp-mask sharpening, a radial vignette and
// animated film grain. The color-grade matrix (brightness/contrast/saturation/
// warmth) is applied by a separate GlowColorMatrix effect earlier in the
// effects pipeline, so this shader works directly on graded RGBA colors.

precision mediump float;
uniform sampler2D uTexSampler;
uniform vec2 uTexSize;
uniform float uSharpen;
uniform float uVignette;
uniform float uGrain;
uniform float uPresentationTimeUs;
varying vec2 vTexSamplingCoord;

// Cheap stateless hash in [0, 1) for the film grain.
float hash(vec2 p) {
  return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
  vec2 uv = vTexSamplingCoord;
  vec4 color = texture2D(uTexSampler, uv);

  if (uSharpen > 0.001) {
    // Unsharp mask: 4-neighbour + center blur, then add back the high-frequency
    // residual scaled by the sharpen amount.
    vec2 texel = 1.0 / uTexSize;
    vec4 blur = texture2D(uTexSampler, uv) * 4.0;
    blur += texture2D(uTexSampler, uv + vec2(-texel.x, 0.0));
    blur += texture2D(uTexSampler, uv + vec2( texel.x, 0.0));
    blur += texture2D(uTexSampler, uv + vec2(0.0, -texel.y));
    blur += texture2D(uTexSampler, uv + vec2(0.0,  texel.y));
    blur *= 0.125;
    color.rgb += uSharpen * (color.rgb - blur.rgb);
  }

  if (uVignette > 0.001) {
    // Darken toward the corners; dist is 0 at centre and ~0.707 at the corners.
    vec2 center = uv - 0.5;
    float dist = length(center);
    float vignette = smoothstep(0.35, 0.7, dist) * uVignette;
    color.rgb *= (1.0 - vignette);
  }

  if (uGrain > 0.001) {
    // Per-pixel noise that shifts over time so it reads as moving film grain
    // rather than a static overlay.
    float t = uPresentationTimeUs * 0.000001;
    vec2 p = uv * uTexSize + t * vec2(61.7, 127.1);
    color.rgb += (hash(p) - 0.5) * uGrain * 0.08;
  }

  gl_FragColor = color;
}
