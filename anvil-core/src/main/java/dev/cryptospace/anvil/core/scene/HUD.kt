package dev.cryptospace.anvil.core.scene

import dev.cryptospace.anvil.core.RenderingSystem
import dev.cryptospace.anvil.core.image.Font
import dev.cryptospace.anvil.core.math.TexturedVertex2
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec4

/**
 * Screen-space overlay for text labels.
 *
 * Label positions are in pixels, measured from the top-left corner of the window to the top-left corner of the text.
 * Labels are drawn after the scene, without depth testing, in the order they were created.
 */
class HUD(
    private val renderingSystem: RenderingSystem,
) {

    private val _labels = mutableListOf<Label>()
    val labels: List<Label>
        get() = _labels

    /**
     * Labels sharing a font and color share a material, so they also share a pipeline and texture binding.
     */
    private val materialsByStyle = mutableMapOf<Pair<TextureId, Vec4>, MaterialId>()

    class Label internal constructor(
        val font: Font,
        val color: Vec4,
        val materialId: MaterialId,
        var position: Vec2,
    ) {
        var text: String = ""
            internal set

        /**
         * Mesh of the current text, or null if the text produces no geometry (e.g. it is empty).
         */
        var meshId: MeshId? = null
            internal set
    }

    fun label(text: String, position: Vec2, font: Font, color: Vec4 = Vec4.one): Label {
        val label = Label(font, color, materialFor(font, color), position)
        rebuildMesh(label, text)
        _labels += label
        return label
    }

    /**
     * Changes a label's text. Rebuilds its mesh only if the text actually changed.
     */
    fun updateLabel(label: Label, text: String) {
        if (label.text != text) {
            rebuildMesh(label, text)
        }
    }

    private fun rebuildMesh(label: Label, text: String) {
        label.meshId?.let(renderingSystem::removeMesh)
        label.text = text

        val mesh = TextLayout.build(label.font, text)
        label.meshId = if (mesh.indices.isEmpty()) {
            null
        } else {
            renderingSystem.uploadMesh(TexturedVertex2, mesh.vertices, mesh.indices)
        }
    }

    fun removeLabel(label: Label) {
        require(_labels.remove(label)) { "Label is not part of this HUD" }
        label.meshId?.let(renderingSystem::removeMesh)
        label.meshId = null
    }

    fun clear() {
        _labels.toList().forEach(::removeLabel)
    }

    private fun materialFor(font: Font, color: Vec4): MaterialId = materialsByStyle.getOrPut(font.textureId to color) {
        renderingSystem.uploadMaterial(
            Material(
                texture = font.textureId,
                properties = MaterialProperties(albedo = color),
                blendMode = BlendMode.ALPHA_BLEND,
                cullMode = CullMode.NONE,
                depthTest = false,
                depthWrite = false,
            ),
        )
    }
}
