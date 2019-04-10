package com.nilhcem.blenamebadge.ui.message

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.nilhcem.blenamebadge.R
import com.nilhcem.blenamebadge.adapter.DrawableAdapter
import com.nilhcem.blenamebadge.core.android.ext.showKeyboard
import com.nilhcem.blenamebadge.core.android.log.Timber
import com.nilhcem.blenamebadge.core.android.viewbinding.bindView
import com.nilhcem.blenamebadge.data.DrawableInfo
import com.nilhcem.blenamebadge.device.model.DataToSend
import com.nilhcem.blenamebadge.device.model.Message
import com.nilhcem.blenamebadge.device.model.Mode
import com.nilhcem.blenamebadge.device.model.Speed
import com.nilhcem.blenamebadge.ui.badge_preview.PreviewBadge
import com.nilhcem.blenamebadge.util.Converters
import com.nilhcem.blenamebadge.util.RecyclerItemClickListenr
import java.util.Timer
import java.util.TimerTask

class MessageActivity : AppCompatActivity() {

    companion object {
        private const val SCAN_TIMEOUT_MS = 10_000L
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_PERMISSION_LOCATION = 1
    }

    private val content: EditText by bindView(R.id.text_to_send)
    private val flash: CheckBox by bindView(R.id.flash)
    private val marquee: CheckBox by bindView(R.id.marquee)
    private val speed: Spinner by bindView(R.id.speed)
    private val mode: Spinner by bindView(R.id.mode)
    private val send: Button by bindView(R.id.send_button)
    private val drawableRecyclerView: RecyclerView by bindView(R.id.recycler_view)
    private val sendByteLoader: ProgressBar by bindView(R.id.sendBytesLoader)
    private val radio: RadioGroup by bindView(R.id.radioGroup)

    private lateinit var drawableRecyclerAdapter: DrawableAdapter

    private val previewBadge: PreviewBadge by bindView(R.id.preview_badge)

    private val presenter by lazy { MessagePresenter() }

    private var previewText: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.message_activity)
        radio.check(R.id.textRadio)

        radio.setOnCheckedChangeListener { group, checkedId ->
            content.isEnabled = radio.checkedRadioButtonId == R.id.textRadio
            previewText = radio.checkedRadioButtonId == R.id.textRadio
        }
        val spinnerItem = R.layout.spinner_item
        speed.adapter = ArrayAdapter<String>(this, spinnerItem, Speed.values().mapIndexed { index, _ -> (index + 1).toString() })
        mode.adapter = ArrayAdapter<String>(this, spinnerItem, Mode.values().map { getString(it.stringResId) })

        send.setOnClickListener {
            val inputManager: InputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE)
                    as InputMethodManager
            inputManager.hideSoftInputFromWindow(content.windowToken, InputMethodManager.SHOW_FORCED)

            if (BluetoothAdapter.getDefaultAdapter().isEnabled) {
                // Easter egg
                if (!content.text.isEmpty()) {
                    send.isEnabled = false
                    val buttonTimer = Timer()
                    buttonTimer.schedule(object : TimerTask() {
                        override fun run() {
                            runOnUiThread {
                                send.isEnabled = true
                                showLoaderView(false)
                            }
                        }
                    }, SCAN_TIMEOUT_MS)
                    presenter.sendMessage(this, convertToDeviceDataModel())
                    showLoaderView(true)
                } else {
                    Toast.makeText(this, "Please enter an item to be previewed", Toast.LENGTH_SHORT).show()
                }
            } else {
                prepareForScan()
            }
        }

        content.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
                updatePreview()
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                ///do nothing
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                //do nothing
            }
        })

        speed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //do nothing
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatePreview()
            }
        }

        mode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //do nothing
            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updatePreview()
            }
        }

        drawableRecyclerView.addOnItemTouchListener(RecyclerItemClickListenr(this, drawableRecyclerView, object : RecyclerItemClickListenr.OnItemClickListener {

            override fun onItemClick(view: View, position: Int) {
                updatePreview()
            }
            override fun onItemLongClick(view: View?, position: Int) {
                //do nothing
            }
        }))

        /*previewButton.setOnClickListener {
            val (valid, textToSend) = presenter.convertToPreview(if (!content.text.isEmpty()) content.text.toString() else " ")
            if (!valid) {
                Toast.makeText(baseContext, R.string.character_not_found, Toast.LENGTH_SHORT).show()
            }
            if (!content.text.isEmpty()) {
                previewBadge.setValue(
                        textToSend,
                        marquee.isChecked,
                        flash.isChecked,
                        Speed.values()[speed.selectedItemPosition],
                        Mode.values()[mode.selectedItemPosition]
                )
            } else {
                previewBadge.setValue(
                        Converters.convertDrawableToLEDHex((drawableRecyclerAdapter.getDefaultItem()).image),
                        false,
                        false,
                        Speed.values()[speed.selectedItemPosition],
                        Mode.FIXED
                )
            }
        }*/

        /*previewButtonDrawable.setOnClickListener {
            val selectedItem = drawableRecyclerAdapter.getSelectedItem()
            if (selectedItem != null)
                previewBadge.setValue(
                        Converters.convertDrawableToLEDHex(selectedItem.image),
                        marquee.isChecked,
                        flash.isChecked,
                        Speed.values()[speed.selectedItemPosition],
                        Mode.values()[mode.selectedItemPosition]
                )
            else
                previewBadge.setValue(
                        Converters.convertDrawableToLEDHex((drawableRecyclerAdapter.getDefaultItem()).image),
                        false,
                        false,
                        Speed.values()[speed.selectedItemPosition],
                        Mode.FIXED
                )
        }*/

        setupRecycler()

        prepareForScan()
    }

    private fun updatePreview() {
        val ledData: List<String>
        if (previewText) {
            val (valid, textToSend) = presenter.convertToPreview(if (!content.text.isEmpty()) content.text.toString() else " ")
            if (!valid) {
                Toast.makeText(baseContext, R.string.character_not_found, Toast.LENGTH_SHORT).show()
            }
            ledData = textToSend
        } else {
            val selectedItem = drawableRecyclerAdapter.getSelectedItem()
            if(selectedItem != null) {
                ledData = Converters.convertDrawableToLEDHex(selectedItem.image) as java.util.ArrayList<String>
            } else {
                ledData = Converters.convertDrawableToLEDHex((drawableRecyclerAdapter.getDefaultItem()).image)
            }
        }
        previewBadge.setValue(
                ledData,
                marquee.isChecked,
                flash.isChecked,
                Speed.values()[speed.selectedItemPosition],
                Mode.values()[mode.selectedItemPosition]
        )
    }

    private fun setupRecycler() {
        drawableRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val listOfDrawables = listOf(
                DrawableInfo(resources.getDrawable(R.drawable.apple)),
                DrawableInfo(resources.getDrawable(R.drawable.clock)),
                DrawableInfo(resources.getDrawable(R.drawable.dustbin)),
                DrawableInfo(resources.getDrawable(R.drawable.face)),
                DrawableInfo(resources.getDrawable(R.drawable.heart)),
                DrawableInfo(resources.getDrawable(R.drawable.home)),
                DrawableInfo(resources.getDrawable(R.drawable.invader)),
                DrawableInfo(resources.getDrawable(R.drawable.mail)),
                DrawableInfo(resources.getDrawable(R.drawable.mix1)),
                DrawableInfo(resources.getDrawable(R.drawable.mix2)),
                DrawableInfo(resources.getDrawable(R.drawable.mushroom)),
                DrawableInfo(resources.getDrawable(R.drawable.mustache)),
                DrawableInfo(resources.getDrawable(R.drawable.oneup)),
                DrawableInfo(resources.getDrawable(R.drawable.pause)),
                DrawableInfo(resources.getDrawable(R.drawable.spider)),
                DrawableInfo(resources.getDrawable(R.drawable.sun)),
                DrawableInfo(resources.getDrawable(R.drawable.thumbs_up))
        )

        drawableRecyclerAdapter = DrawableAdapter(this, listOfDrawables)
        drawableRecyclerView.adapter = drawableRecyclerAdapter
    }

    override fun onResume() {
        super.onResume()
        content.requestFocus()
        content.showKeyboard()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                showAlertDialog(true)
                return
            } else if (resultCode == Activity.RESULT_OK) {
                prepareForScan()
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Timber.d { "Location permission accepted" }
            } else {
                showAlertDialog(false)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun showLoaderView(status: Boolean) {
        runOnUiThread {
            if (status) {
                sendByteLoader.visibility = View.VISIBLE
                send.isEnabled = false
                send.visibility = View.GONE
            } else {
                sendByteLoader.visibility = View.GONE
                send.isEnabled = true
                send.visibility = View.VISIBLE
            }
        }
    }

    private fun showAlertDialog(bluetoothDialog: Boolean) {
        val dialogMessage = if (bluetoothDialog) getString(R.string.enable_bluetooth) else getString(R.string.grant_location_permission)
        val builder = AlertDialog.Builder(this)
        builder.setIcon(resources.getDrawable(R.drawable.ic_caution))
        builder.setTitle(getString(R.string.permission_required))
        builder.setMessage(dialogMessage)
        builder.setPositiveButton("OK") { _, _ ->
            prepareForScan()
        }
        builder.setNegativeButton("CANCEL") { _, _ ->
            Toast.makeText(this, R.string.enable_bluetooth, Toast.LENGTH_SHORT).show()
        }
        builder.create().show()
    }

    private fun convertToDeviceDataModel(): DataToSend {
        return DataToSend(listOf(Message(content.text.trim().toString(), flash.isChecked, marquee.isChecked, Speed.values()[speed.selectedItemPosition], Mode.values()[mode.selectedItemPosition])))
    }

    private fun convertToDeviceDataModelNull(): DataToSend {
        return DataToSend(listOf(Message("", flash.isChecked, marquee.isChecked, Speed.values()[speed.selectedItemPosition], Mode.values()[mode.selectedItemPosition])))
    }

    private fun prepareForScan() {
        if (isBleSupported()) {
            // Ensures Bluetooth is enabled on the device
            val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val btAdapter = btManager.adapter
            if (btAdapter.isEnabled) {
                // Prompt for runtime permission
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    Timber.i { "Coarse permission granted" }
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), REQUEST_PERMISSION_LOCATION)
                }
            } else {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
        } else {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun isBleSupported(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
    }
}
