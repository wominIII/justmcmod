#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;

in vec2 texCoord;

uniform vec2  ScreenSize;
uniform int   SoundCount;
uniform float SoundScreenPos[32];
uniform float SoundRadii[16];
uniform float SoundDist[16];
uniform float SoundColors[48];
uniform int   RenderMode;

out vec4 fragColor;

float edgeDetectColor(vec2 uv) {
    vec2 px = vec2(1.0 / ScreenSize.x, 1.0 / ScreenSize.y);

    float tl = dot(texture(DiffuseSampler, uv + vec2(-px.x,  px.y)).rgb, vec3(0.299, 0.587, 0.114));
    float tc = dot(texture(DiffuseSampler, uv + vec2(  0.0,  px.y)).rgb, vec3(0.299, 0.587, 0.114));
    float tr = dot(texture(DiffuseSampler, uv + vec2( px.x,  px.y)).rgb, vec3(0.299, 0.587, 0.114));
    float ml = dot(texture(DiffuseSampler, uv + vec2(-px.x,   0.0)).rgb, vec3(0.299, 0.587, 0.114));
    float mr = dot(texture(DiffuseSampler, uv + vec2( px.x,   0.0)).rgb, vec3(0.299, 0.587, 0.114));
    float bl = dot(texture(DiffuseSampler, uv + vec2(-px.x, -px.y)).rgb, vec3(0.299, 0.587, 0.114));
    float bc = dot(texture(DiffuseSampler, uv + vec2(  0.0, -px.y)).rgb, vec3(0.299, 0.587, 0.114));
    float br = dot(texture(DiffuseSampler, uv + vec2( px.x, -px.y)).rgb, vec3(0.299, 0.587, 0.114));

    float gx = -tl - 2.0*ml - bl + tr + 2.0*mr + br;
    float gy = -tl - 2.0*tc - tr + bl + 2.0*bc + br;

    return clamp(sqrt(gx*gx + gy*gy) * 3.0, 0.0, 1.0);
}

// Helper: 转换非线性深度到线性深度 (假设 near=0.05, far=1000.0)
float linearizeDepth(float d) {
    float n = 0.05;
    float f = 1000.0;
    float zc = 2.0 * d - 1.0;
    return (2.0 * n * f) / (f + n - zc * (f - n));
}

// 基于深度缓冲的边缘检测，只提取几何轮廓，忽略贴图细节
float edgeDetectDepth(vec2 uv) {
    // 再次增大采样间距，让线条变得更少且只描绘大轮廓
    vec2 px = vec2(2.5 / ScreenSize.x, 2.5 / ScreenSize.y);
    
    // 读取周围像素的线性深度值
    float center = linearizeDepth(texture(DepthSampler, uv).r);
    
    float tl = linearizeDepth(texture(DepthSampler, uv + vec2(-px.x,  px.y)).r);
    float tc = linearizeDepth(texture(DepthSampler, uv + vec2(  0.0,  px.y)).r);
    float tr = linearizeDepth(texture(DepthSampler, uv + vec2( px.x,  px.y)).r);
    float ml = linearizeDepth(texture(DepthSampler, uv + vec2(-px.x,   0.0)).r);
    float mr = linearizeDepth(texture(DepthSampler, uv + vec2( px.x,   0.0)).r);
    float bl = linearizeDepth(texture(DepthSampler, uv + vec2(-px.x, -px.y)).r);
    float bc = linearizeDepth(texture(DepthSampler, uv + vec2(  0.0, -px.y)).r);
    float br = linearizeDepth(texture(DepthSampler, uv + vec2( px.x, -px.y)).r);

    // 计算中心差分
    float diff1 = abs((tl + br) - 2.0 * center);
    float diff2 = abs((tr + bl) - 2.0 * center);
    float diff3 = abs((tc + bc) - 2.0 * center);
    float diff4 = abs((ml + mr) - 2.0 * center);

    float maxDiff = max(max(diff1, diff2), max(diff3, diff4));

    // 提高阈值，进一步过滤掉地面的微小起伏，只抓取真正的方块边缘
    float threshold = 0.5 + (center * 0.1);

    // 平滑阶跃，这样线条边缘也会稍微柔和一点，没那么生硬
    float edge = smoothstep(threshold * 0.8, threshold * 1.2, maxDiff);
    
    // 忽略天空
    if (texture(DepthSampler, uv).r > 0.999) return 0.0;

    return edge;
}

void main() {
    // ── Mode 2: WIREFRAME (Robot Vision) ──
    if (RenderMode == 2) {
        vec3 sceneColor = texture(DiffuseSampler, texCoord).rgb;
        float edge = edgeDetectColor(texCoord);
        float luminance = dot(sceneColor, vec3(0.299, 0.587, 0.114));

        float scanline = sin(texCoord.y * ScreenSize.y * 3.14159) * 0.04;
        
        vec2 centerDist = texCoord - vec2(0.5);
        float vignette = 1.0 - dot(centerDist, centerDist) * 1.2;
        vignette = smoothstep(0.0, 1.0, vignette);

        float maxC = max(sceneColor.r, max(sceneColor.g, sceneColor.b));
        bool isRed   = (maxC > 0.05 && sceneColor.r > sceneColor.g * 3.0 && sceneColor.r > sceneColor.b * 3.0);
        bool isGreen = (maxC > 0.05 && sceneColor.g > sceneColor.r * 3.0 && sceneColor.g > sceneColor.b * 3.0);
        bool isCyan  = (maxC > 0.05 && sceneColor.g > sceneColor.r * 3.0
                        && sceneColor.b > sceneColor.r * 3.0 && sceneColor.b > 0.03
                        && abs(sceneColor.g - sceneColor.b) < maxC * 0.5);
        // 检测粉色/紫红色 (Red 和 Blue 都比 Green 高)
        bool isPink  = (maxC > 0.05 && sceneColor.r > sceneColor.g * 1.5 && sceneColor.b > sceneColor.g * 1.5);

        vec3 finalColor;

        if (isCyan) {
            finalColor = vec3(0.0, 0.8, 1.0) * (edge * 1.5 + sceneColor.g * 0.5);
        } else if (isRed || isGreen || isPink) {
            vec3 boosted = sceneColor * 1.2;
            finalColor = boosted + vec3(edge * 0.5);
        } else {
            vec3 tintColor = vec3(1.0, 1.0, 1.0); // 白灰色调
            vec3 edgeColor = vec3(1.0, 1.0, 1.0); // 纯白线

            vec3 bgColor = tintColor * (luminance * 0.2 + 0.05);
            finalColor = bgColor + (edge * edgeColor * 2.0);
        }

        finalColor = (finalColor + vec3(scanline)) * vignette;

        fragColor = vec4(finalColor, 1.0);
        return;
    }

    // ── Mode 1: STATIC WAVE VISION (Echolocation Replacement) ──
    float depth = texture(DepthSampler, texCoord).r;
    float edge = edgeDetectDepth(texCoord); // 使用基于深度的边缘检测
    
    // Approximate distance from depth buffer (assuming near plane = 0.05)
    float ndc = depth * 2.0 - 1.0;
    float dist = 0.1 / max(1.0 - ndc, 0.00001);
    
    if (depth >= 1.0) {
        dist = 10000.0; // Sky
    }

    // Fog calculation matching WaveVisionRenderer (12.0 to 48.0)
    float fogStart = 12.0;
    float fogEnd = 48.0;
    float fogFactor = clamp((dist - fogStart) / (fogEnd - fogStart), 0.0, 1.0);

    vec3 sceneColor = texture(DiffuseSampler, texCoord).rgb;
    float luminance = dot(sceneColor, vec3(0.299, 0.587, 0.114));
    
    // 不勾勒任何线条，仅保留原始画面的灰度暗淡版本
    vec3 bgColor = vec3(luminance * 0.15); // 非常暗的灰度环境光
    
    // finalColor 等于环境底色（没有任何边缘线条了）
    vec3 finalColor = bgColor;
    
    // 依然应用基于深度的纯黑迷雾，让远处融入黑暗
    finalColor = mix(finalColor, vec3(0.0, 0.0, 0.0), fogFactor);
    
    fragColor = vec4(finalColor, 1.0);
}
