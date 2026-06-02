package com.puzzle.photo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.puzzle.photo.data.PuzzleData
import com.puzzle.photo.databinding.ActivityPuzzleBinding
import com.puzzle.photo.utils.ImageAnalyzer

class PuzzleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPuzzleBinding
    private val analyzer = ImageAnalyzer()
    private var currentStep = 0
    private var hintVisible = false

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else Toast.makeText(this, "Camera permission is required to play", Toast.LENGTH_LONG).show()
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val path = result.data?.getStringExtra(CameraActivity.EXTRA_PHOTO_PATH)
            if (path != null) analyzePhoto(path)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPuzzleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        showStep(0)

        binding.btnTakePhoto.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                cameraPermission.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnHint.setOnClickListener {
            hintVisible = !hintVisible
            binding.tvHint.visibility = if (hintVisible) View.VISIBLE else View.GONE
            binding.btnHint.text = if (hintVisible) "Hide Hint" else "Show Hint"
        }
    }

    private fun showStep(index: Int) {
        currentStep = index
        val step = PuzzleData.steps[index]

        binding.tvStepNumber.text = "Step ${step.stepNumber} of ${PuzzleData.steps.size}"
        binding.progressBar.max = PuzzleData.steps.size
        binding.progressBar.progress = index
        binding.tvClue.text = step.clue
        binding.tvHint.text = "Hint: ${step.hint}"
        binding.tvHint.visibility = View.GONE
        binding.tvFeedback.visibility = View.GONE
        binding.btnHint.text = "Show Hint"
        binding.btnTakePhoto.isEnabled = true
        hintVisible = false
    }

    private fun launchCamera() {
        cameraLauncher.launch(Intent(this, CameraActivity::class.java))
    }

    private fun analyzePhoto(path: String) {
        binding.btnTakePhoto.isEnabled = false
        binding.tvFeedback.text = "Analysing photo..."
        binding.tvFeedback.setTextColor(getColor(android.R.color.darker_gray))
        binding.tvFeedback.visibility = View.VISIBLE

        val step = PuzzleData.steps[currentStep]

        analyzer.analyze(
            context = this,
            photoPath = path,
            onResult = { labels ->
                runOnUiThread {
                    if (analyzer.matches(labels, step.acceptableLabels)) {
                        showCorrect(step.successMessage)
                    } else {
                        showWrong()
                    }
                }
            },
            onError = {
                runOnUiThread {
                    binding.btnTakePhoto.isEnabled = true
                    binding.tvFeedback.text = "Could not analyse photo — try again."
                    binding.tvFeedback.setTextColor(getColor(android.R.color.holo_red_dark))
                }
            }
        )
    }

    private fun showCorrect(message: String) {
        binding.tvFeedback.text = "✔ $message"
        binding.tvFeedback.setTextColor(getColor(android.R.color.holo_green_dark))

        binding.root.postDelayed({
            val next = currentStep + 1
            if (next >= PuzzleData.steps.size) {
                startActivity(Intent(this, SuccessActivity::class.java))
                finish()
            } else {
                showStep(next)
            }
        }, 2200)
    }

    private fun showWrong() {
        binding.btnTakePhoto.isEnabled = true
        binding.tvFeedback.text = "✗ Not quite right — look around and try again!"
        binding.tvFeedback.setTextColor(getColor(android.R.color.holo_red_dark))
    }
}
