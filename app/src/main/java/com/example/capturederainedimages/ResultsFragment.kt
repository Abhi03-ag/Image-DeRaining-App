package com.example.capturederainedimages

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import  android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.os.Bundle
import android.os.SystemClock
import android.view.*
import androidx.fragment.app.Fragment
import com.example.capturederainedimages.ml.GanTfliteFp161
import kotlinx.android.synthetic.main.fragment_results.*
import kotlinx.coroutines.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.model.Model
import java.io.File
import java.io.FileInputStream
import java.util.*


class ResultsFragment : Fragment(), View.OnClickListener {

  private  var currentPhotoPath: String? = null
  private val parentJob = Job()
    private val coroutineScope = CoroutineScope(
        Dispatchers.Main + parentJob
    )

    private fun getOutputAsync(bitmap: Bitmap): Deferred<Pair<Bitmap, Long>> =
        // use async() to create a coroutine in an IO optimized Dispatcher for model inference
        coroutineScope.async(Dispatchers.IO) {

            // GPU delegate
            val options = Model.Options.Builder()
                .setDevice(Model.Device.GPU)
                .setNumThreads(4)
                .build()

            // Input
            val sourceImage = TensorImage.fromBitmap(bitmap)

            // Output
            val derainedImage: TensorImage
            val startTime = SystemClock.uptimeMillis()
            // model inferencing
            derainedImage = inferenceWithFp16Model(sourceImage)


            // Note this inference time includes pre-processing and post-processing
            val inferenceTime = SystemClock.uptimeMillis() - startTime
            val derainedImageBitmap = derainedImage.bitmap

            return@async Pair(derainedImageBitmap, inferenceTime)
        }

    private fun inferenceWithFp16Model(sourceImage: TensorImage?): TensorImage {

        val model = GanTfliteFp161.newInstance(requireContext())


        // Runs model inference and gets result.
        val outputs = sourceImage?.let { model.process(it) }
        val derainedImage = outputs!!.derainedImageAsTensorImage

        // Releases model resources if no longer used.
        model.close()
        return derainedImage

    }
    private fun updateUI(outputBitmap: Bitmap, inferenceTime: Long) {
        progressbar.visibility = View.GONE
        imageview_output?.setImageBitmap(outputBitmap)
//       makeText(context, inferenceTime.toString(), LENGTH_SHORT).show()
        inference_info.setText("Inference time: " + inferenceTime.toString() + "ms")
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        currentPhotoPath = requireArguments().getString("root_dir")


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Try.setOnClickListener(this)
        if(currentPhotoPath!=null) {
            val photoFile = File(currentPhotoPath)


            val options = BitmapFactory.Options()
            val inputBitmap = FileInputStream(photoFile).use { imageStream ->
                BitmapFactory.decodeStream(imageStream, null, options)
            }
            val resized = Bitmap.createScaledBitmap(inputBitmap!!, 256, 256, true)
            val rotatedImage = rotateImageIfRequired(resized,photoFile)
            imageview_input?.setImageBitmap(rotatedImage)
            coroutineScope.launch(Dispatchers.Main) {
                val (outputBitmap, inferenceTime) = getOutputAsync(rotatedImage).await()
                updateUI(outputBitmap, inferenceTime)
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        //clean up coroutine job
        parentJob.cancel()
    }
    private fun rotateImageIfRequired(img: Bitmap, imageFile: File): Bitmap {
        val ei = FileInputStream(imageFile).use { ExifInterface(it) }
        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_UNDEFINED
        )

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degree)
        //img.recycle()
        return Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
    }

    override fun onClick(v: View?) {
        when(v!!.id)
        {
            R.id.Try -> requireActivity().onBackPressed()
        }
    }


}