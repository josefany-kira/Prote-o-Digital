package com.hackerkira.protecao

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val senhaCorreta = "hackerkira1234"
    private val pacoteChamadas = "com.android.dialer"
    private var monitorando = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 100, 64, 100)
            gravity = Gravity.CENTER
        }

        val titulo = TextView(this).apply {
            text = "Proteção HackerKira"
            textSize = 22f
            gravity = Gravity.CENTER
        }

        val edtSenha = EditText(this).apply {
            hint = "Digite a senha"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val btnAtivar = Button(this).apply { text = "Ativar Proteção" }
        val status = TextView(this)

        layout.addView(titulo)
        layout.addView(edtSenha)
        layout.addView(btnAtivar)
        layout.addView(status)
        setContentView(layout)

        val data: Uri? = intent?.data
        if (data != null && data.toString().contains("ativar-protecao")) {
            exibirTermos {
                iniciarProtecao()
            }
        }

        btnAtivar.setOnClickListener {
            if (edtSenha.text.toString() == senhaCorreta) {
                iniciarProtecao()
                status.text = "Proteção Ativada"
            } else {
                status.text = "Senha incorreta"
            }
        }
    }

    private fun exibirTermos(onAceito: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Termos de Uso")
            .setMessage("Ao continuar, todos os aplicativos serão protegidos. Somente chamadas serão liberadas.")
            .setPositiveButton("Aceitar") { _, _ -> onAceito() }
            .setNegativeButton("Cancelar") { _, _ -> finish() }
            .show()
    }

    private fun iniciarProtecao() {
        if (!temPermissaoUso()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else if (!monitorando) {
            monitorando = true
            monitorarApps()
        }
    }

    private fun temPermissaoUso(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            "android:get_usage_stats",
            Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun monitorarApps() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                val pacoteAtual = getAppEmUso()
                if (pacoteAtual != null &&
                    pacoteAtual != packageName &&
                    pacoteAtual != pacoteChamadas
                ) {
                    pedirSenhaParaAbrir(pacoteAtual)
                }
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun getAppEmUso(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val tempo = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, tempo - 5000, tempo)
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun pedirSenhaParaAbrir(appBloqueado: String) {
        runOnUiThread {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 64, 32, 64)
                gravity = Gravity.CENTER
            }

            val info = TextView(this).apply {
                text = "App protegido: $appBloqueado"
            }

            val edtSenha = EditText(this).apply {
                hint = "Digite a senha"
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            val btnConfirmar = Button(this).apply {
                text = "Desbloquear"
            }

            val alerta = AlertDialog.Builder(this)
                .setTitle("Proteção Ativada")
                .setView(layout)
                .setCancelable(false)
                .create()

            btnConfirmar.setOnClickListener {
                if (edtSenha.text.toString() == senhaCorreta) {
                    alerta.dismiss()
                } else {
                    Toast.makeText(this, "Senha incorreta", Toast.LENGTH_SHORT).show()
                }
            }

            layout.addView(info)
            layout.addView(edtSenha)
            layout.addView(btnConfirmar)

            alerta.show()
        }
    }
}
