package dev.cryptospace.anvil.core

import dev.cryptospace.anvil.core.math.TexturedVertex3
import dev.cryptospace.anvil.core.math.Vec2
import dev.cryptospace.anvil.core.math.Vec3
import dev.cryptospace.anvil.core.rendering.Mesh
import org.lwjgl.assimp.AIMesh
import org.lwjgl.assimp.AIVector3D
import org.lwjgl.assimp.Assimp
import org.lwjgl.system.MemoryUtil
import java.io.InputStream

class ModelManager(
    private val renderingSystem: RenderingSystem,
) {

    fun loadModel(inputStream: InputStream): List<Mesh> {
        val size = inputStream.available()
        val byteBuffer = MemoryUtil.memAlloc(size)
            .put(inputStream.readNBytes(size))
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

            checkNotNull(scene) { error("Failed to load model") }
            val meshes = scene.mMeshes()

            checkNotNull(meshes) { error("Failed to load model meshes") }
            val numMeshes = scene.mNumMeshes()
            val result = mutableListOf<Mesh>()

            for (i in 0 until numMeshes) {
                val mesh = AIMesh.createSafe(meshes[i])
                checkNotNull(mesh) { error("Failed to load model mesh $i") }

                val resultingMesh = loadMesh(mesh, renderingSystem)
                result.add(resultingMesh)
            }

            return result
        } finally {
            MemoryUtil.memFree(byteBuffer)
        }
    }

    private fun loadMesh(mesh: AIMesh, renderingSystem: RenderingSystem): Mesh {
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

        return renderingSystem.uploadMesh(TexturedVertex3::class, vertices, indices)
    }
}
