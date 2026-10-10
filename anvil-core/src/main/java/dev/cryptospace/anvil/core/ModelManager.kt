package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
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
        val numFaces = mesh.mNumFaces()
        val numVertices = mesh.mNumVertices()
        val vertices = Array(numVertices) { index ->
            val vertex = mesh.mVertices()[index]

            val position = Vec3(vertex.x(), vertex.y(), vertex.z())
            val mTextureCoords: AIVector3D.Buffer? = mesh.mTextureCoords(0)
            val texture = if (mTextureCoords == null) {
                Vec2.zero
            } else {
                Vec2(mTextureCoords[index].x(), mTextureCoords[index].y())
            }

            TexturedVertex3(position, Vec3.one, texture)
        }

        val indices = Array(numFaces * 3) { index ->
            val face = mesh.mFaces()[index / 3]
            val numIndices = face.mNumIndices()
            check(numIndices == 3) { "Expected 3 indices per face, got $numIndices" }
            val index = face.mIndices()[index % 3].toUInt()
            index
        }

        return renderingSystem.uploadMesh(TexturedVertex3, vertices, indices)
    }
}
