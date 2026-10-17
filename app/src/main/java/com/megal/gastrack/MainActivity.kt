
package com.megal.gastrack
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
class MainActivity: AppCompatActivity(){
 override fun onCreate(s: Bundle?){
  super.onCreate(s)
  val tv = TextView(this); tv.text="GasTrack Rocha - Device 7/8\nConectado a megal-erp Turso"; tv.textSize=18f
  setContentView(tv)
 }
}
