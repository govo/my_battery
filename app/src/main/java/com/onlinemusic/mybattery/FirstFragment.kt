package com.onlinemusic.mybattery

import android.R
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.*
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import com.onlinemusic.mybattery.databinding.FragmentFirstBinding
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val amList = mutableListOf<String>()
    private var adapter: ArrayAdapter<String>? = null
    private var maxTemp: Int = 350
    private var minTemp: Int = 50
    private var vibeCount: Int = 0
    private var vibeCountMax: Int = 10


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    fun toStatus(code: Int): String {
        val s = when (code) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "充电"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "放电"
            BatteryManager.BATTERY_STATUS_FULL -> "满电"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "不在充电"
            BatteryManager.BATTERY_STATUS_UNKNOWN -> "未知"
            else -> "未知代码"
        }
        return s
    }

    fun toHealthString(code: Int): String {
        val s = when (code) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "良好"
            BatteryManager.BATTERY_HEALTH_COLD -> "过冷"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "过热"
            BatteryManager.BATTERY_HEALTH_DEAD -> "失效"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "电压过高"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "获取失败"
            BatteryManager.BATTERY_HEALTH_UNKNOWN -> "未知"
            else -> "未知状态"
        }
        return s
    }

    fun playVibe() {
        val vibrator =
            context?.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.EFFECT_TICK))
        } else {
            vibrator.vibrate(100)
        }
    }

    fun toDisplayNum(n: Int, r: Int): String {
        if (n == 0) return "0"
        var p = if (n < 0) "-" else ""
        var nn = if (n < 0) n * -1 else n
        return "${p}${nn / r}.${nn % r}"
    }

    fun toDisplayNum(n: Long, r: Long): String {
        if (n == 0L) return "0"
        var p = if (n < 0) "-" else ""
        var nn = if (n < 0) n * -1 else n
        return "${p}${nn / r}.${nn % r}"
    }

    fun updateBattery() {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context?.registerReceiver(null, ifilter)
        }
        val batteryManager: BatteryManager =
            context?.getSystemService(Context.BATTERY_SERVICE) as BatteryManager;
        val cc = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        var ca = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE);
        ca = if (ca < 0) 0 else ca;
        val cn = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
        val bs = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
        val cap = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        var ec = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER);
        val ecStr = if (ec < 0) "无法读取" else toDisplayNum(ec, 1000);

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
//        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
//        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        val chargeType = if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB) "USB" else "充电头";

        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val health: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val temperature: Int =
            batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        if (temperature >= maxTemp || temperature <= minTemp) {
            if (vibeCount % vibeCountMax == 0) {
                playVibe()
            }
            if (vibeCount >= vibeCountMax) {
                vibeCount = 0
            }
            vibeCount += 1
        } else if (vibeCount > 0) {
            vibeCount = 0
        }

        val low: Boolean =
            batteryStatus?.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false) ?: false
//        val cycle: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1 //API 34, android14
        val icon: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, -1) ?: -1
        val present = batteryStatus?.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false) ?: false
        val technology =
            batteryStatus?.getBooleanExtra(BatteryManager.EXTRA_TECHNOLOGY, false) ?: false
        val volatile: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1

        var cAmp = toDisplayNum(cn, 1000) + "mA"
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val second = c.get(Calendar.SECOND)
        amList.add(0, "$hour:$minute:$second\n$cAmp")
        adapter?.notifyDataSetChanged();

        binding.textviewFirst.text = """
电池状态:        
是否在充电: $isCharging
充电模式: $chargeType
当前电量水平: $level
总电量水平: $scale
建康状态: ${toHealthString(health)}
剩余百分比: $cap%
*温度: ${
            toDisplayNum(
                temperature,
                10
            )
        }℃ ${if (temperature >= maxTemp) " ‼️温度过高‼️" else if (temperature <= minTemp) " 🥶温度过低🥶" else " ✅(" + minTemp/10 + "-" + maxTemp/10+"℃)"}
电压: ${toDisplayNum(volatile, 1000)}V
是否低电量: $low
循环次数：Android14可能
剩余电量: ${toDisplayNum(cc, 1000)}mAh
平均电流: ${toDisplayNum(ca, 1000)}mA
当前电流: $cAmp
电池状态: ${toStatus(bs)}
剩余能量（瓦时）: $ecStr
展示: $present 技术: $technology
右边为当前电流=====`>
负数充电，正数放电
""".trimMargin()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ArrayAdapter<String>(requireContext(), R.layout.simple_list_item_1, amList)
        binding.amList.adapter = adapter
        updateBattery()
        binding.textviewFirst.setOnClickListener {
            updateBattery()
        }
        val timer = object : CountDownTimer(60000 * 60 * 24, 500) {
            override fun onTick(millisUntilFinished: Long) {
                updateBattery()
            }

            override fun onFinish() {}
        }
        timer.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}