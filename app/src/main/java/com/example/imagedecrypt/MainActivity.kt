package com.example.imagedecrypt

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import android.util.Base64
import java.io.File
import java.io.FileOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainActivity : AppCompatActivity() {

    private lateinit var etKey: EditText
    private lateinit var tvFile: TextView
    private var selectedUri: Uri? = null
    private val REQ_FILE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etKey = findViewById(R.id.et_key)
        tvFile = findViewById(R.id.tv_file)
        findViewById<Button>(R.id.btn_select).setOnClickListener { openFilePicker() }
        findViewById<Button>(R.id.btn_decrypt).setOnClickListener { doDecrypt() }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, REQ_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_FILE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                selectedUri = uri
                val doc = DocumentFile.fromSingleUri(this, uri)
                tvFile.text = "已选择：${doc?.name}"
            }
        }
    }

    private fun doDecrypt() {
        val keyB64 = etKey.text.toString().trim()
        if (keyB64.isBlank()) {
            toast("请输入Base64密钥")
            return
        }
        val uri = selectedUri ?: run {
            toast("请先选择 picture.enc")
            return
        }

        Thread {
            try {
                val stream = contentResolver.openInputStream(uri)!!
                val iv = ByteArray(16)
                stream.read(iv)
                val encBytes = stream.readBytes()
                stream.close()

                val keyBytes: ByteArray = Base64.decode(keyB64, Base64.NO_WRAP)
                val secretKey = SecretKeySpec(keyBytes, "AES")
                val ivSpec = IvParameterSpec(iv)

                val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
                val plain = cipher.doFinal(encBytes)

                val outFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "out_dec.png")
                FileOutputStream(outFile).use { it.write(plain) }

                runOnUiThread { toast("✅解密成功！保存至:\n${outFile.absolutePath}") }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread { toast("❌解密失败：${e.message}") }
            }
        }.start()
    }

    private fun toast(msg: String) {
        runOnUiThread { Toast.makeText(this, msg, Toast.LENGTH_LONG).show() }
    }
}