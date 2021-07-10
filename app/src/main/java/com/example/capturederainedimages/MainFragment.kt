package com.example.capturederainedimages

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.image_picker_dialog.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainFragment : Fragment(),View.OnClickListener {
    private var navController: NavController? = null
    lateinit var uri: Uri
    private val REQUEST_PERMISSION = 100
    private val REQUEST_STORAGE_PERMISSION = 101
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PICK_IMAGE = 2
    private  lateinit   var ImageFile:File

    var currentPhotoPath: String? = null

    @Throws(IOException::class)
    private fun createCapturedPhoto(): File {
        val timestamp: String = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("PHOTO_${timestamp}", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        floating_button.setOnClickListener(this)
        image_view.setOnClickListener(this)

        view.findViewById<Button>(R.id.view_results_btn).setOnClickListener(this)
        currentPhotoPath = null

    }

    private fun ChooseImageDialog() {
        val mDialogView = LayoutInflater.from(requireActivity()).inflate(R.layout.image_picker_dialog, null)
        val mBuilder = AlertDialog.Builder(requireActivity())
            .setView(mDialogView)
            .setTitle("Pick Image")
            .setCancelable(false)

        val  mAlertDialog = mBuilder.show()
        mAlertDialog.setCanceledOnTouchOutside(true)

        mDialogView.CameraPick.setOnClickListener {
            mAlertDialog.dismiss()

            if (context?.let {
                    ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.CAMERA
                    )
                }
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            } else {
                openCamera()
            }

        }

        mDialogView.GalleryPick.setOnClickListener {
            mAlertDialog.dismiss()

            if (context?.let {
                    ContextCompat.checkSelfPermission(
                        it,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
                != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            } else {
                openGallery()
            }
        }

        mDialogView.cancel_dialog.setOnClickListener {
            mAlertDialog.dismiss()

        }

    }

    override fun onResume() {
        super.onResume()
        checkCameraPermission()
        checkStoragePermission()
    }
    private fun checkStoragePermission()
    {
        if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),REQUEST_STORAGE_PERMISSION)
        }
    }
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == REQUEST_PERMISSION)
        {
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(requireContext(),"Camera Permission Granted",Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(requireContext(),"Camera Permission denied",Toast.LENGTH_SHORT).show()
            }
        }
        else if(requestCode==REQUEST_STORAGE_PERMISSION)
        {
            if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(requireContext(),"Storage Permission Granted",Toast.LENGTH_SHORT).show()
            }
            else
            {
                Toast.makeText(requireContext(),"Storage Permission denied",Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {

            R.id.view_results_btn -> {
                if (context?.let {
                        ContextCompat.checkSelfPermission(
                            it,
                            Manifest.permission.CAMERA
                        )
                    }
                    != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
                } else {
                    if (image_view.drawable != null && currentPhotoPath !=null && ImageFile.exists()) {

                      val bundle = bundleOf("root_dir" to currentPhotoPath)
                        navController!!.navigate(R.id.action_mainFragment_to_resultsFragment,bundle)
                    } else {
                        Toast.makeText(context, "Choose the Input Image ", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            }
            R.id.floating_button -> {
                ChooseImageDialog()
            }
            R.id.image_view -> {
                ChooseImageDialog()
            }
        }
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            intent.resolveActivity(requireContext().packageManager)?.also {
                val photoFile: File? = try {
                    createCapturedPhoto()
                } catch (ex: IOException) {
                    null
                }
                photoFile?.also {
                    val photoURI = FileProvider.getUriForFile(
                        requireContext(),
                        "${BuildConfig.APPLICATION_ID}.fileprovider",
                        it
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
                }

            }
        }
    }


    private fun openGallery() {
        Intent(Intent.ACTION_GET_CONTENT).also { intent ->
            intent.type = "image/*"
            intent.resolveActivity(requireContext().packageManager)?.also {
                startActivityForResult(intent, REQUEST_PICK_IMAGE)
            }
        }
    }

    fun Intent?.getFilePath(context: Context): String {
        return this?.data?.let { data -> URIPathHelper().getPath(context, data) ?: "" } ?: ""
    }

    fun Uri?.getFilePath(context: Context): String {
        return this?.let { uri -> URIPathHelper().getPath(context, uri) ?: "" } ?: ""
    }

    fun ClipData.Item?.getFilePath(context: Context): String {
        return this?.uri?.getFilePath(context) ?: ""
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                uri = Uri.parse(currentPhotoPath)
                image_view.setImageURI(uri)
                ImageFile = File(currentPhotoPath)

            } else if (requestCode == REQUEST_PICK_IMAGE && data != null) {
                uri = data.data!!
                currentPhotoPath = uri.getFilePath(requireContext())
                ImageFile = File(currentPhotoPath)
                image_view.setImageURI(uri)
            }
        }
    }

}

