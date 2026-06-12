package com.example.pametneucionice.augmentedreality

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.example.pametneucionice.databinding.ActivityAugmentedRealityBinding
import com.example.pametneucionice.network.RetrofitProvider
import com.example.pametneucionice.network.RoomApiService
import com.google.ar.core.ArCoreApk
import com.google.ar.core.AugmentedImage
import com.google.ar.core.AugmentedImageDatabase
import com.google.ar.core.TrackingState
import io.github.sceneview.ar.arcore.getUpdatedAugmentedImages
import io.github.sceneview.ar.arcore.isTracking
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.node.ImageNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class AugmentedRealityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAugmentedRealityBinding
    private lateinit var roomApiService: RoomApiService
    private val panelBitmapRenderer = PanelBitmapRenderer()
    private val trackedAnchors = mutableMapOf<String, AnchorNode>()
    private val panelNodes = mutableMapOf<String, ImageNode>()
    private val panelOffsets = mutableMapOf<String, Float>()
    private val pollingJobs = mutableMapOf<String, Job>()
    private var activeImageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAugmentedRealityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topInset
            }
            insets
        }

        roomApiService = RetrofitProvider.createRoomApiService("http://localhost:8080")

        binding.buttonClose.setOnClickListener {
            finish()
        }
        binding.statusText.text = "Pronađi marker učionice."

        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            finishBecauseUnsupported()
            return
        }

        binding.sceneView.apply {
            lifecycle = this@AugmentedRealityActivity.lifecycle
            planeRenderer.isEnabled = false
            configureSession { session, config ->
                val augmentedImageDatabase = AugmentedImageDatabase(session)
                val markerFileNames = assets.list("markers").orEmpty()
                    .filter { it.endsWith(".png") }
                markerFileNames.forEach { markerFileName ->
                    val imageName = markerFileName.substringBeforeLast('.')
                    try {
                        assets.open("markers/$markerFileName").use { markerStream ->
                            val markerBitmap = BitmapFactory.decodeStream(markerStream)
                            if (markerBitmap != null) {
                                augmentedImageDatabase.addImage(
                                    imageName,
                                    markerBitmap,
                                    MARKER_PHYSICAL_WIDTH_METERS
                                )
                            }
                        }
                    } catch (exception: Exception) {
                        Log.w(
                            "AugmentedRealityActivity",
                            "Preskočen marker $markerFileName",
                            exception
                        )
                    }
                }
                config.augmentedImageDatabase = augmentedImageDatabase
                if (augmentedImageDatabase.numImages == 0) {
                    setStatusText("Nema markera u bazi aplikacije.")
                }
            }
            onSessionUpdated = { _, frame ->
                frame.getUpdatedAugmentedImages().forEach { augmentedImage ->
                    when {
                        augmentedImage.isTracking -> onAugmentedImageTracking(augmentedImage)
                        augmentedImage.trackingState == TrackingState.STOPPED ->
                            removePanel(augmentedImage.name)
                    }
                }
            }
            onSessionFailed = { exception ->
                Log.w("AugmentedRealityActivity", "Neuspešno pokretanje AR sesije", exception)
                finishBecauseUnsupported()
            }
        }
    }

    private fun onAugmentedImageTracking(augmentedImage: AugmentedImage) {
        val imageName = augmentedImage.name
        if (imageName == activeImageName) {
            return
        }
        activeImageName?.let { previousImageName ->
            removePanel(previousImageName)
        }
        activeImageName = imageName
        val anchor = augmentedImage.createAnchor(augmentedImage.centerPose)
        val anchorNode = AnchorNode(binding.sceneView.engine, anchor)
        anchorNode.isPositionEditable = false
        binding.sceneView.addChildNode(anchorNode)
        trackedAnchors[imageName] = anchorNode
        panelOffsets[imageName] =
            -(augmentedImage.extentZ / 2f + PANEL_BOTTOM_GAP_METERS + PANEL_HEIGHT_METERS / 2f)

        val roomId = imageName.substringAfterLast('_')
        startPolling(imageName, roomId)
        setStatusText("Učionica $roomId — podaci uživo.")
    }

    private fun removePanel(imageName: String) {
        pollingJobs.remove(imageName)?.cancel()
        panelOffsets.remove(imageName)
        val panelNode = panelNodes.remove(imageName)
        val anchorNode = trackedAnchors.remove(imageName)
        if (panelNode != null && anchorNode != null) {
            anchorNode.removeChildNode(panelNode)
            panelNode.destroy()
        }
        if (anchorNode != null) {
            binding.sceneView.removeChildNode(anchorNode)
            anchorNode.destroy()
        }
        if (imageName == activeImageName) {
            activeImageName = null
        }
    }

    private fun startPolling(imageName: String, roomId: String) {
        pollingJobs.remove(imageName)?.cancel()
        pollingJobs[imageName] = lifecycleScope.launch {
            while (isActive) {
                try {
                    val room = roomApiService.getRoom(roomId)
                    val panelBitmap =
                        panelBitmapRenderer.render(this@AugmentedRealityActivity, room)
                    updatePanel(imageName, panelBitmap)
                    setStatusText("Učionica $roomId — podaci uživo.")
                } catch (exception: IOException) {
                    setStatusText("Server nije dostupan. Proveri adresu servera.")
                } catch (exception: HttpException) {
                    setStatusText("Server nije dostupan. Proveri adresu servera.")
                }
                delay(POLLING_INTERVAL_MILLISECONDS)
            }
        }
    }

    private fun updatePanel(imageName: String, panelBitmap: Bitmap) {
        val anchorNode = trackedAnchors[imageName] ?: return
        val existingPanelNode = panelNodes[imageName]
        if (existingPanelNode != null) {
            existingPanelNode.bitmap = panelBitmap
            existingPanelNode.texture.generateMipmaps(binding.sceneView.engine)
            return
        }
        val panelNode = ImageNode(
            materialLoader = binding.sceneView.materialLoader,
            bitmap = panelBitmap,
            size = Size(PANEL_WIDTH_METERS, PANEL_HEIGHT_METERS, 0f)
        )
        panelNode.rotation = Rotation(x = -90f, y = 0f, z = 0f)
        panelNode.position = Position(0f, 0f, panelOffsets[imageName] ?: DEFAULT_PANEL_OFFSET_METERS)
        anchorNode.addChildNode(panelNode)
        panelNodes[imageName] = panelNode
    }

    private fun setStatusText(message: String) {
        if (binding.statusText.text.toString() == message) {
            return
        }
        binding.statusText.animate()
            .alpha(0f)
            .setDuration(75)
            .withEndAction {
                binding.statusText.text = message
                binding.statusText.animate().alpha(1f).setDuration(75).start()
            }
            .start()
    }

    private fun finishBecauseUnsupported() {
        setStatusText("Ovaj uređaj ne podržava ARCore.")
        lifecycleScope.launch {
            delay(2500)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        pollingJobs.values.forEach { it.cancel() }
        pollingJobs.clear()
    }

    override fun onResume() {
        super.onResume()
        trackedAnchors.keys.forEach { imageName ->
            startPolling(imageName, imageName.substringAfterLast('_'))
        }
    }

    companion object {
        const val MARKER_PHYSICAL_WIDTH_METERS = 0.21f
        const val PANEL_WIDTH_METERS = 0.32f
        const val PANEL_HEIGHT_METERS = 0.24f
        const val PANEL_BOTTOM_GAP_METERS = 0.03f
        const val DEFAULT_PANEL_OFFSET_METERS = -0.27f
        const val POLLING_INTERVAL_MILLISECONDS = 3000L
    }
}
