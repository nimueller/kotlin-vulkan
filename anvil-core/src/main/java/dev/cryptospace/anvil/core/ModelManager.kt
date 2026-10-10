package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.scene.Face
import dev.cryptospace.anvil.core.scene.MeshId
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AIVector3D
import org.lwjgl.assimp.Assimp
import org.lwjgl.system.MemoryUtil
import java.io.InputStream

class ModelManager(
    private val renderingSystem: RenderingSystem,
) {

    fun loadModel(inputStream: InputStream): List<MeshId> {
        // available() is only an estimate for many streams (e.g. jar resources), so read everything
        val bytes = inputStream.readAllBytes()
        val byteBuffer = MemoryUtil.memAlloc(bytes.size)
            .put(bytes)
            .flip()
        try {
            val scene = Assimp.aiImportFileFromMemory(
                byteBuffer,
                Assimp.aiProcess_CalcTangentSpace or
                    Assimp.aiProcess_Triangulate or
                    Assimp.aiProcess_JoinIdenticalVertices or
                    Assimp.aiProcess_FlipUVs or
                    Assimp.aiProcess_SortByPType,
                null as CharSequence?,
            )

            checkNotNull(scene) { "Failed to load model: ${Assimp.aiGetErrorString()}" }

            try {
                val meshes = checkNotNull(scene.mMeshes()) { "Model contains no meshes" }

                return List(scene.mNumMeshes()) { index ->
                    val mesh = checkNotNull(AIMesh.createSafe(meshes[index])) { "Failed to load model mesh $index" }
                    loadMesh(mesh, renderingSystem)
                }
            } finally {
                Assimp.aiReleaseImport(scene)
            }
        } finally {
            MemoryUtil.memFree(byteBuffer)
        }
    }

    private fun loadMesh(mesh: AIMesh, renderingSystem: RenderingSystem): MeshId {
        val positions = mesh.mVertices()
        val textureCoordinates: AIVector3D.Buffer? = mesh.mTextureCoords(0)
        val faces = mesh.mFaces()

        val indices = IntArray(mesh.mNumFaces() * Face.INDICES_SIZE)
        for (faceIndex in 0 until mesh.mNumFaces()) {
            val face = faces[faceIndex]
            check(face.mNumIndices() == Face.INDICES_SIZE) { "Expected 3 indices per face, got ${face.mNumIndices()}" }
            val faceIndices = face.mIndices()
            for (corner in 0 until Face.INDICES_SIZE) {
                indices[faceIndex * Face.INDICES_SIZE + corner] = faceIndices[corner]
            }
        }

        // Stream vertex data straight from Assimp's memory into the upload buffer, without intermediate objects
        val stride = TexturedVertex3.byteSize
        return renderingSystem.uploadMesh(TexturedVertex3, mesh.mNumVertices(), indices) { buffer ->
            for (index in 0 until mesh.mNumVertices()) {
                val offset = index * stride
                val position = positions[index]
                TexturedVertex3.position.put(buffer, offset, position.x(), position.y(), position.z())
                TexturedVertex3.color.put(buffer, offset, 1f, 1f, 1f)

                if (textureCoordinates == null) {
                    TexturedVertex3.texture.put(buffer, offset, 0f, 0f)
                } else {
                    val textureCoordinate = textureCoordinates[index]
                    TexturedVertex3.texture.put(buffer, offset, textureCoordinate.x(), textureCoordinate.y())
                }
            }
        }
    }
}
