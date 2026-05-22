package com.gabrielformento.neuronioamil

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statsManager: StatsManager
    private lateinit var imgBackground: ImageView
    private var bgmPlayer: MediaPlayer? = null
    private var abaAtual = "stats"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statsManager = StatsManager(this)
        imgBackground = findViewById(R.id.imgBackground)

        configurarBotoesGlobais()
        configurarBotoesMenu()
        configurarModalEstatisticas()
        atualizarVisualDinamico()
    }

    override fun onResume() {
        super.onResume()
        atualizarVisualDinamico()
        atualizarBotaoSom()
        iniciarMusicaFundo()
    }

    override fun onPause() {
        super.onPause()
        bgmPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        bgmPlayer?.release()
        bgmPlayer = null
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun getCorTemaHex(): String {
        return when (statsManager.temaAtual) {
            "azul" -> "#00eaff"
            "amarelo" -> "#FFD700"
            "verde" -> "#34c759"
            "vermelho" -> "#ff3b30"
            "lilas" -> "#bf5af2"
            "ciano" -> "#5ac8fa"
            "laranja" -> "#ff9500"
            else -> "#00eaff"
        }
    }

    private fun criarFundoBotao(corFundo: String = "#33000000"): GradientDrawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 50f
        shape.setColor(Color.parseColor(corFundo))
        shape.setStroke(3, Color.parseColor(getCorTemaHex()))
        return shape
    }

    private fun criarFundoModal(): GradientDrawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 40f
        shape.setColor(Color.parseColor("#E6111111"))
        shape.setStroke(5, Color.parseColor(getCorTemaHex()))
        return shape
    }

    private fun atualizarVisualDinamico() {
        val nomeImagem = "${statsManager.temaAtual}_background"
        val idImagem = resources.getIdentifier(nomeImagem, "drawable", packageName)
        if (idImagem != 0) imgBackground.setImageResource(idImagem)

        findViewById<TextView>(R.id.txtTituloMenu).setTextColor(Color.parseColor(getCorTemaHex()))

        findViewById<LinearLayout>(R.id.painelMenuPrincipal).background = criarFundoModal()
        findViewById<LinearLayout>(R.id.painelEstatisticas).background = criarFundoModal()

        val btnTemas = findViewById<Button>(R.id.btnTemas)
        val btnPadrao = findViewById<Button>(R.id.btnPadrao)
        val btnSobrevivencia = findViewById<Button>(R.id.btnSobrevivencia)
        val btnSair = findViewById<Button>(R.id.btnSair)
        val btnSom = findViewById<Button>(R.id.btnSom)
        val btnSobre = findViewById<Button>(R.id.btnSobre)
        val btnFecharStats = findViewById<Button>(R.id.btnFecharStats)
        val btnEstatisticas = findViewById<Button>(R.id.btnEstatisticas)

        btnTemas.background = criarFundoBotao()
        btnPadrao.background = criarFundoBotao()
        btnSobrevivencia.background = criarFundoBotao()
        btnSair.background = criarFundoBotao("#80FF3B30")
        btnSom.background = criarFundoBotao()
        btnSobre.background = criarFundoBotao()
        btnFecharStats.background = criarFundoBotao("#FF3B30")

        btnEstatisticas.background = criarFundoBotao(getCorTemaHex())
        btnEstatisticas.setTextColor(Color.BLACK)

        trocarAba(abaAtual)
    }

    private fun iniciarMusicaFundo() {
        if (statsManager.somAtivo) {
            if (bgmPlayer == null) {
                val idSom = resources.getIdentifier("menuprincipal", "raw", packageName)
                if (idSom != 0) {
                    bgmPlayer = MediaPlayer.create(this, idSom)
                    bgmPlayer?.isLooping = true
                }
            }
            bgmPlayer?.start()
        } else {
            bgmPlayer?.pause()
        }
    }

    private fun atualizarBotaoSom() {
        val btnSom = findViewById<Button>(R.id.btnSom)
        if (statsManager.somAtivo) {
            btnSom.text = "🔊 SOM ON"
            btnSom.setTextColor(Color.WHITE)
        } else {
            btnSom.text = "🔇 SOM OFF"
            btnSom.setTextColor(Color.parseColor("#FF3B30"))
        }
    }

    private fun configurarBotoesGlobais() {
        val btnSom = findViewById<Button>(R.id.btnSom)
        val btnSobre = findViewById<Button>(R.id.btnSobre)
        val btnSair = findViewById<Button>(R.id.btnSair)

        btnSom.setOnClickListener {
            statsManager.somAtivo = !statsManager.somAtivo
            atualizarBotaoSom()
            iniciarMusicaFundo()
        }

        btnSobre.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val img = ImageView(this)
            var idSobre = resources.getIdentifier("${statsManager.temaAtual}_sobre", "drawable", packageName)
            if (idSobre == 0) idSobre = resources.getIdentifier("sobre", "drawable", packageName)

            if (idSobre != 0) img.setImageResource(idSobre)
            img.layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            img.adjustViewBounds = true

            dialog.setContentView(img)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)

            img.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }

        btnSair.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.background = criarFundoModal()
            layout.setPadding(dpToPx(25), dpToPx(25), dpToPx(25), dpToPx(25))
            layout.gravity = Gravity.CENTER

            val txt = TextView(this)
            txt.text = "Deseja realmente sair do jogo?"
            txt.setTextColor(Color.WHITE)
            txt.textSize = 18f
            txt.typeface = resources.getFont(R.font.comic)
            txt.gravity = Gravity.CENTER
            txt.setPadding(0, 0, 0, dpToPx(20))

            val btnSim = Button(this)
            btnSim.text = "Sim, Sair"
            btnSim.background = criarFundoBotao("#FF3B30")
            btnSim.setTextColor(Color.WHITE)
            btnSim.typeface = resources.getFont(R.font.comic)
            btnSim.setOnClickListener { finishAffinity() }

            val btnNao = Button(this)
            btnNao.text = "Cancelar"
            btnNao.background = criarFundoBotao()
            btnNao.setTextColor(Color.WHITE)
            btnNao.typeface = resources.getFont(R.font.comic)
            btnNao.setOnClickListener { dialog.dismiss() }

            layout.addView(txt)
            layout.addView(btnSim)

            val espaco = Space(this)
            espaco.layoutParams = LinearLayout.LayoutParams(1, dpToPx(10))
            layout.addView(espaco)
            layout.addView(btnNao)

            dialog.setContentView(layout)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.show()
        }
    }

    private fun configurarBotoesMenu() {
        val btnPadrao = findViewById<Button>(R.id.btnPadrao)
        val btnSobrevivencia = findViewById<Button>(R.id.btnSobrevivencia)
        val btnTemas = findViewById<Button>(R.id.btnTemas)

        btnPadrao.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("MODO", "padrao")
            startActivity(intent)
        }

        btnSobrevivencia.setOnClickListener {
            val intent = Intent(this, QuizActivity::class.java)
            intent.putExtra("MODO", "sobrevivencia")
            startActivity(intent)
        }

        btnTemas.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val scroll = ScrollView(this)
            scroll.background = criarFundoModal()
            scroll.isVerticalScrollBarEnabled = false

            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))

            val titulo = TextView(this)
            titulo.text = "Escolha um Tema"
            titulo.setTextColor(Color.parseColor(getCorTemaHex()))
            titulo.textSize = 20f
            titulo.typeface = resources.getFont(R.font.comic)
            titulo.gravity = Gravity.CENTER
            titulo.setPadding(0, 0, 0, dpToPx(15))
            layout.addView(titulo)

            val temas = listOf("azul", "amarelo", "verde", "vermelho", "lilas", "ciano", "laranja")

            temas.forEach { tema ->
                val btn = Button(this)
                btn.text = tema.uppercase()
                btn.background = criarFundoBotao()
                btn.setTextColor(Color.WHITE)
                btn.typeface = resources.getFont(R.font.comic)
                btn.setOnClickListener {
                    statsManager.temaAtual = tema
                    atualizarVisualDinamico()
                    dialog.dismiss()
                }
                layout.addView(btn)

                val espaco = Space(this)
                espaco.layoutParams = LinearLayout.LayoutParams(1, dpToPx(8))
                layout.addView(espaco)
            }

            scroll.addView(layout)
            dialog.setContentView(scroll)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.show()
        }
    }

    private fun configurarModalEstatisticas() {
        val btnEstatisticas = findViewById<Button>(R.id.btnEstatisticas)
        val modalEstatisticas = findViewById<FrameLayout>(R.id.modalEstatisticas)
        val btnFecharStats = findViewById<Button>(R.id.btnFecharStats)

        val abaStatsBtn = findViewById<Button>(R.id.abaStatsBtn)
        val abaRecordesBtn = findViewById<Button>(R.id.abaRecordesBtn)
        val abaConquistasBtn = findViewById<Button>(R.id.abaConquistasBtn)

        val txtStatsAcertos = findViewById<TextView>(R.id.txtStatsAcertos)
        val txtStatsErros = findViewById<TextView>(R.id.txtStatsErros)
        val txtStatsPrecisao = findViewById<TextView>(R.id.txtStatsPrecisao)
        val txtStatsPartidas = findViewById<TextView>(R.id.txtStatsPartidas)
        val txtListaRecordes = findViewById<TextView>(R.id.txtListaRecordes)

        btnEstatisticas.setOnClickListener {
            val acertos = statsManager.acertosTotais
            val erros = statsManager.errosTotais
            val total = acertos + erros
            val precisao = if (total > 0) (acertos * 100) / total else 0

            txtStatsAcertos.text = "Acertos: $acertos"
            txtStatsErros.text = "Erros: $erros"
            txtStatsPrecisao.text = "Precisão: $precisao%"
            txtStatsPartidas.text = "Partidas: ${statsManager.partidasJogadas}"

            val recordes = statsManager.getTop5()
            if (recordes.isEmpty()) {
                txtListaRecordes.text = "Nenhum recorde registrado."
            } else {
                var textoRecordes = ""
                recordes.forEachIndexed { index, rec ->
                    textoRecordes += "${index + 1}º | ${rec.nome} - ${rec.pontos} pts\n\n"
                }
                txtListaRecordes.text = textoRecordes
            }

            carregarMuralConquistas()
            modalEstatisticas.visibility = View.VISIBLE
        }

        btnFecharStats.setOnClickListener {
            modalEstatisticas.visibility = View.GONE
        }

        abaStatsBtn.setOnClickListener { trocarAba("stats") }
        abaRecordesBtn.setOnClickListener { trocarAba("recordes") }
        abaConquistasBtn.setOnClickListener { trocarAba("conquistas") }
    }

    private fun trocarAba(abaAtiva: String) {
        abaAtual = abaAtiva

        val abaStatsBtn = findViewById<Button>(R.id.abaStatsBtn)
        val abaRecordesBtn = findViewById<Button>(R.id.abaRecordesBtn)
        val abaConquistasBtn = findViewById<Button>(R.id.abaConquistasBtn)

        val conteudoStats = findViewById<LinearLayout>(R.id.conteudoStats)
        val conteudoRecordes = findViewById<ScrollView>(R.id.conteudoRecordes)
        val conteudoConquistas = findViewById<ScrollView>(R.id.conteudoConquistas)

        abaStatsBtn.background = criarFundoBotao("#333333")
        abaRecordesBtn.background = criarFundoBotao("#333333")
        abaConquistasBtn.background = criarFundoBotao("#333333")

        abaStatsBtn.setTextColor(Color.WHITE)
        abaRecordesBtn.setTextColor(Color.WHITE)
        abaConquistasBtn.setTextColor(Color.WHITE)

        conteudoStats.visibility = View.GONE
        conteudoRecordes.visibility = View.GONE
        conteudoConquistas.visibility = View.GONE

        val corTemaSelecionada = getCorTemaHex()
        when (abaAtiva) {
            "stats" -> {
                abaStatsBtn.background = criarFundoBotao(corTemaSelecionada)
                abaStatsBtn.setTextColor(Color.BLACK)
                conteudoStats.visibility = View.VISIBLE
            }
            "recordes" -> {
                abaRecordesBtn.background = criarFundoBotao(corTemaSelecionada)
                abaRecordesBtn.setTextColor(Color.BLACK)
                conteudoRecordes.visibility = View.VISIBLE
            }
            "conquistas" -> {
                abaConquistasBtn.background = criarFundoBotao(corTemaSelecionada)
                abaConquistasBtn.setTextColor(Color.BLACK)
                conteudoConquistas.visibility = View.VISIBLE
            }
        }
    }

    private fun carregarMuralConquistas() {
        val grid = findViewById<GridLayout>(R.id.gridConquistas)
        grid.removeAllViews()

        // Ajuste de margem interna do grid para evitar corte nas bordas
        grid.setPadding(dpToPx(10), dpToPx(10), dpToPx(10), dpToPx(10))

        val nomesConquistas = listOf(
            "Primeiros Passos", "Explorador", "Curioso", "Sábio Universal", "De Volta ao Jogo",
            "Consistência", "Viciado em Quiz", "Rei do Quiz", "Deus do Quiz", "Quase Lá",
            "Olho de Águia", "Imparável", "Cirúrgico", "Rápido no Gatilho", "Velocista Mental",
            "Sem Hesitar", "Evolução Constante", "Sem Ajuda", "Modo Hardcore", "Sem tempo a perder"
        )

        val descricoes = listOf(
            "Complete seu primeiro quiz", "Jogue quizzes de 3 categorias", "Jogue quizzes de 5 categorias", "Jogue quizzes de 10+ categorias", "Jogar 2 dias seguidos",
            "Jogar 5 dias seguidos", "Jogar 10 dias seguidos", "Jogue mais de 50 quizzes", "Jogue mais de 100 quizzes", "Erre apenas 1 pergunta no modo padrão",
            "Acerte 5 perguntas seguidas", "Acerte 10 perguntas seguidas", "Acerte 20 perguntas seguidas", "Responda em menos de 3 segundos", "Responda em menos de 2 segundos",
            "Responda em menos de 1 segundo", "Melhore pontuação em 3 quizzes seguidos", "Sobrevivência: Zerar sem usar 50/50", "Sobrevivência: Zerar sem 50/50, pular, freeze", "Sobrevivência: Zerar sem ajudas"
        )

        for (i in 1..20) {
            val img = ImageView(this)
            val params = GridLayout.LayoutParams()

            params.width = dpToPx(48)
            params.height = dpToPx(48)
            params.setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6))
            img.layoutParams = params

            val jaGanhou = statsManager.temConquista(i)
            val numStr = i.toString().padStart(2, '0')
            val nomeImg = if (jaGanhou) "badge_$numStr" else "b_badge_$numStr"

            val idRes = resources.getIdentifier(nomeImg, "drawable", packageName)
            if (idRes != 0) img.setImageResource(idRes)

            img.setOnClickListener {
                val dialog = Dialog(this)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                val layout = LinearLayout(this)
                layout.orientation = LinearLayout.VERTICAL
                layout.background = criarFundoModal()
                layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))

                val txtTitulo = TextView(this)
                txtTitulo.text = nomesConquistas[i-1]
                txtTitulo.setTextColor(Color.parseColor(getCorTemaHex()))
                txtTitulo.textSize = 20f
                txtTitulo.typeface = resources.getFont(R.font.comic)
                txtTitulo.gravity = Gravity.CENTER
                layout.addView(txtTitulo)

                val txtDesc = TextView(this)
                txtDesc.text = descricoes[i-1] + if(jaGanhou) "\n\n✅ DESBLOQUEADA!" else "\n\n❌ BLOQUEADA"
                txtDesc.setTextColor(Color.WHITE)
                txtDesc.textSize = 16f
                txtDesc.typeface = resources.getFont(R.font.comic)
                txtDesc.gravity = Gravity.CENTER
                txtDesc.setPadding(0, dpToPx(10), 0, dpToPx(15))
                layout.addView(txtDesc)

                val btnOk = Button(this)
                btnOk.text = "Fechar"
                btnOk.background = criarFundoBotao()
                btnOk.setTextColor(Color.WHITE)
                btnOk.typeface = resources.getFont(R.font.comic)
                btnOk.setOnClickListener { dialog.dismiss() }
                layout.addView(btnOk)

                dialog.setContentView(layout)
                dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                dialog.show()
            }

            grid.addView(img)
        }
    }
}