## Supported glTF Material Property

Below is the table of availability for non-extension material properties from glTF V2 in Vanilla or when ShaderMod activated.

For ShaderMod support. This will require a shader pack that support **specular map** and **normal map**, and a corresponding **glTF Material Converter Pack** that combine each property to those map.

For how to set up shader pack and glTF Material Converter Pack, please see [Enabling PBR effects of glTF models through shader pack](enabling_pbr_material_of_gltf_models_through_shader_pack.md) tutorial.

| Material Property                                          | Vanilla | ShaderMod |
|------------------------------------------------------------|:-------:|:---------:|
| pbrMetallicRoughness > baseColorFactor                     |    ✅    |     ✅     |
| pbrMetallicRoughness > baseColorTexture > index            |    ✅    |     ✅     |
| pbrMetallicRoughness > baseColorTexture > texCoord         |    ✅    |     ✅     |
| pbrMetallicRoughness > metallicFactor                      |    ❌    |     ✅     |
| pbrMetallicRoughness > roughnessFactor                     |    ❌    |     ✅     |
| pbrMetallicRoughness > metallicRoughnessTexture > index    |    ❌    |     ✅     |
| pbrMetallicRoughness > metallicRoughnessTexture > texCoord |    ❌    |     ❌     |
| normalTexture > index                                      |    ❌    |     ✅     |
| normalTexture > texCoord                                   |    ❌    |     ❌     |
| normalTexture > scale                                      |    ❌    |     ✅     |
| occlusionTexture > index                                   |    ✅    |     ✅     |
| occlusionTexture > texCoord                                |    ❌    |     ❌     |
| occlusionTexture > strength                                |    ✅    |     ✅     |
| emissiveTexture > index                                    |    ✅    |     ✅     |
| emissiveTexture > texCoord                                 |    ✅    |     ❌     |
| emissiveFactor                                             |    ✅    |     ✅     |
| alphaMode                                                  |    ✅    |     ✅     |
| alphaCutoff                                                |    ✅    |     ✅     |
| doubleSided                                                |    ✅    |     ✅     |

## glTF Material Extras Property for ShaderMod

To fully utilized custom material format provided by shader pack, you can specify these properties in `materials` > `extras` > `crgltf` > `daxShader`.

| Material Extras Property | Description                                                                                                                                                                                                                                                                                                                                                      |
|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| colorMapTexture          | When specified, bypass the **color map** output of the glTF Material Converter Pack.                                                                                                                                                                                                                                                                             |
| normalMapTexture         | When specified, bypass the **normal map** output of the glTF Material Converter Pack.                                                                                                                                                                                                                                                                            |
| specularMapTexture       | When specified, bypass the **specular map** output of the glTF Material Converter Pack.                                                                                                                                                                                                                                                                          |
| mc_midTexCoord           | The texture coordinate index to a `TEXCOORD_n` for `mc_midTexCoord` attribute from ShaderMod.</br>For parallax mapping to working, you need to manually create the second texture coordinate from uv unwrapper of your 3D modeling program.</br>And setting all UV points of each face on the second texture coordinate to the center of the face or face group. |

Below is an example structure in a glTF file.
```json
{
  "materials": [
    {
      "extras": {
        "crgltf": {
          "daxShader": {
            "colorMapTexture": 0,
            "normalMapTexture": 1,
            "specularMapTexture": 2,
            "mc_midTexCoord": 0
          }
        }
      }
    }
  ]
}
```