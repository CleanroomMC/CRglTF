# CRglTF
Full-featured glTF model renderer for [Cleanroom](https://github.com/CleanroomMC/Cleanroom).
> The status of this mod is still considered as WIP - API breaking change or deprecation might happen in the future.

## Usages
> The documentation for how to use with this mod is TBD, please check out the example usage for now.

The example codes for rendering Block, Item, and Entity
- https://github.com/TimLee9024/CRglTF-Example
## Features
- [x] GLTF format
- [x] GLB format
- [x] UVs
- [x] Normals
- [x] Tangents[^1]
- [x] Vertex colors[^2]
- [x] Materials[^3]
- [x] Textures
- [x] Mutiple texture coordinates[^3]
- [x] Animations[^4]
- [x] Rig[^5]
- [x] Morph targets[^6]
- [x] Zero-scale node culling[^7]

## Optimization
This mod support various optimization through 3 profiles of OpenGL availability. By default, it will automatically select for you.

| Profile        | Vertex Array Object<br/>(VAO) | Hardware Accelerated<br/>Skinning | Hardware Accelerated<br/>Morphing | Status |       System<br/>Requirements       |
|----------------|:-----------------------------:|:---------------------------------:|:---------------------------------:|--------|:-----------------------------------:|
| `Full`         |               ✅               |                 ✅                 |                 ✅                 | ✅ Done |             OpenGL 4.3              |
| `macOS_Legacy` |               ✅               |                 ✅                 |                 ✅                 | 🚧 WIP |     OpenGL 2.1 +<br/>Extensions     |
| `GL21_FBO`     |               ❌               |                 ❌                 |                 ❌                 | ✅ Done | OpenGL 2.1 +<br/>Framebuffer Object |

## Credit
- JglTF by javagl : https://github.com/javagl/JglTF
- Mikk Tangent Generator by jMonkeyEngine : https://github.com/jMonkeyEngine/jmonkeyengine

[^1]: Only available for ShaderMod from OptiFine.
[^2]: Only `COLOR_0` can be used, it is treated as vertex color of `baseColor`.
[^3]: Please see [Supported glTF Material Property](https://github.com/TimLee9024/CRglTF/blob/master/docs/supported_gltf_material_property.md) for more info.
[^4]: Support every interpolation method.
[^5]: Support more than 4 bone per-vertex.
[^6]: Support every valid attribute, including `COLOR_n` and `TEXCOORD_n`.
[^7]: Support both non-skinned mesh and skinned mesh.
