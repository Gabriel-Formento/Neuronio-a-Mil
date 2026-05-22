package com.gabrielformento.neuronioamil

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class QuizActivity : AppCompatActivity() {

    private lateinit var statsManager: StatsManager
    private var perguntasLista: MutableList<Question> = mutableListOf()
    private var indiceAtual = 0
    private var timer: CountDownTimer? = null
    private var tempoRestanteMs: Long = 10000
    private var respondido = false
    private var indiceCorretoGlobal = -1

    private var perguntaPlayer: MediaPlayer? = null
    private var timerPlayer: MediaPlayer? = null

    // Sistema de Pontos e Sobrevivência
    private var pontuacaoAtual = 0
    private var multiplicador = 1
    private var comboRapido = 0
    private var acertosNaPartida = 0
    private var modoJogo = "padrao"

    private var usou5050 = false
    private var usouFreeze = false
    private var usouSkip = false

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        statsManager = StatsManager(this)
        modoJogo = intent.getStringExtra("MODO") ?: "padrao"

        atualizarVisualDinamico()
        configurarBotaoVoltar()
        configurarLifelines()
        carregarPerguntas()

        if (modoJogo == "padrao") {
            findViewById<LinearLayout>(R.id.timerArea).visibility = View.INVISIBLE
            findViewById<TextView>(R.id.txtPergunta).visibility = View.INVISIBLE
            findViewById<LinearLayout>(R.id.layoutOpcoes).visibility = View.INVISIBLE
            iniciarShowDeTVPadrao()
        } else {
            findViewById<TextView>(R.id.txtPontuacao).visibility = View.VISIBLE
            proximaPergunta()
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }


    // Sistema de Cores e Bordas Dinâmicas

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

    private fun criarFundoBotao(isOpcao: Boolean = false, corOverride: String? = null): GradientDrawable {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 50f

        val corFundo = corOverride ?: if (isOpcao) "#B3000000" else "#33000000"
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
        val imgBackground = findViewById<ImageView>(R.id.imgQuizBackground)
        val nomeImagem = "${statsManager.temaAtual}_background"
        val idImagem = resources.getIdentifier(nomeImagem, "drawable", packageName)
        if (idImagem != 0) imgBackground.setImageResource(idImagem)

        findViewById<Button>(R.id.btnVoltar).background = criarFundoBotao()

        val botoes = listOf<Button>(
            findViewById(R.id.opt0), findViewById(R.id.opt1),
            findViewById(R.id.opt2), findViewById(R.id.opt3)
        )
        botoes.forEach { it.background = criarFundoBotao(isOpcao = true) }
    }

    // Botão de Voltar e Ajudas

    private fun configurarBotaoVoltar() {
        val btnVoltar = findViewById<Button>(R.id.btnVoltar)
        btnVoltar.setOnClickListener {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.background = criarFundoModal()
            layout.setPadding(dpToPx(25), dpToPx(25), dpToPx(25), dpToPx(25))
            layout.gravity = Gravity.CENTER

            val txt = TextView(this)
            txt.text = "Abandonar a partida em andamento?"
            txt.setTextColor(Color.WHITE)
            txt.textSize = 18f
            txt.typeface = resources.getFont(R.font.comic)
            txt.gravity = Gravity.CENTER
            txt.setPadding(0, 0, 0, dpToPx(20))

            val btnSim = Button(this)
            btnSim.text = "Sim, Abandonar"
            btnSim.background = criarFundoBotao(corOverride = "#FF3B30")
            btnSim.setTextColor(Color.WHITE)
            btnSim.typeface = resources.getFont(R.font.comic)
            btnSim.setOnClickListener {
                pararAudiosAtivos()
                timer?.cancel()
                dialog.dismiss()
                finish()
            }

            val btnNao = Button(this)
            btnNao.text = "Continuar Jogando"
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

    private fun configurarLifelines() {
        val layoutAjudas = findViewById<LinearLayout>(R.id.layoutAjudas)
        val btn5050 = findViewById<ImageView>(R.id.btnAjuda5050)
        val btnFreeze = findViewById<ImageView>(R.id.btnAjudaFreeze)
        val btnSkip = findViewById<ImageView>(R.id.btnAjudaSkip)

        if (modoJogo == "sobrevivencia") {
            layoutAjudas.visibility = View.VISIBLE

            btn5050.setOnClickListener {
                if (!usou5050 && !respondido) {
                    usou5050 = true
                    btn5050.alpha = 0.3f
                    tocarSom("plim")
                    aplicar5050()
                }
            }

            btnFreeze.setOnClickListener {
                if (!usouFreeze && !respondido) {
                    usouFreeze = true
                    btnFreeze.alpha = 0.3f
                    tocarSom("plim")
                    iniciarTimer(tempoRestanteMs + 5000)
                }
            }

            btnSkip.setOnClickListener {
                if (!usouSkip && !respondido) {
                    usouSkip = true
                    btnSkip.alpha = 0.3f
                    tocarSom("plim")
                    pararAudiosAtivos()
                    timer?.cancel()
                    indiceAtual++
                    proximaPergunta()
                }
            }
        }
    }

    private fun aplicar5050() {
        val botoes = listOf<Button>(
            findViewById(R.id.opt0), findViewById(R.id.opt1),
            findViewById(R.id.opt2), findViewById(R.id.opt3)
        )
        val erradas = mutableListOf(0, 1, 2, 3)
        erradas.remove(indiceCorretoGlobal)
        erradas.shuffle()

        botoes[erradas[0]].text = ""
        botoes[erradas[0]].isClickable = false
        botoes[erradas[1]].text = ""
        botoes[erradas[1]].isClickable = false
    }

    // Sistema Anti-Repetição de Perguntas
    private fun puxarPerguntasFila(todas: List<Question>, nivel: String?, qtd: Int): List<Question> {
        val candidatas = if (nivel != null) todas.filter { it.nivel == nivel } else todas
        val vistas = statsManager.getPerguntasVistas()

        var disponiveis = candidatas.filter { !vistas.contains(it.texto) }

        if (disponiveis.size < qtd) {
            val textosCandidatas = candidatas.map { it.texto }.toSet()
            vistas.removeAll(textosCandidatas)
            statsManager.setPerguntasVistas(vistas)
            disponiveis = candidatas
        }

        val selecionadas = disponiveis.shuffled().take(qtd)

        val novasVistas = statsManager.getPerguntasVistas()
        novasVistas.addAll(selecionadas.map { it.texto })
        statsManager.setPerguntasVistas(novasVistas)

        return selecionadas
    }

    private fun carregarPerguntas() {
        val jsonString = assets.open("questions.json").bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<Question>>() {}.type
        val todasPerguntas: List<Question> = Gson().fromJson(jsonString, listType)

        if (modoJogo == "padrao") {
            perguntasLista.addAll(puxarPerguntasFila(todasPerguntas, "facil", 2))
            perguntasLista.addAll(puxarPerguntasFila(todasPerguntas, "medio", 2))
            perguntasLista.addAll(puxarPerguntasFila(todasPerguntas, "dificil", 1))
        } else {
            perguntasLista.addAll(puxarPerguntasFila(todasPerguntas, null, 100))
        }
    }

    // Sistema de Roteiro

    private fun mostrarAssetInterludio(nomeImg: String, nomeSom: String, duracaoMs: Long, proximoPasso: () -> Unit) {
        val overlay = findViewById<FrameLayout>(R.id.overlayInterludio)
        val img = findViewById<ImageView>(R.id.imgInterludio)

        val nomeCompletoImg = "${statsManager.temaAtual}_$nomeImg"
        val idImagem = resources.getIdentifier(nomeCompletoImg, "drawable", packageName)

        if (idImagem != 0) {
            img.setImageResource(idImagem)
            overlay.visibility = View.VISIBLE
        }

        tocarSom(nomeSom)

        mainHandler.postDelayed({
            overlay.visibility = View.GONE
            proximoPasso()
        }, duracaoMs)
    }

    private fun iniciarShowDeTVPadrao() {
        mostrarAssetInterludio("fundo2", "plim", 1500) {
            mostrarAssetInterludio("imagem0", "som0", 3500) {
                mostrarAssetInterludio("imagem1", "som1", 4000) {
                    mostrarAssetInterludio("facil", "som2", 1500) {
                        mostrarAssetInterludio("pergunta1", "som3", 1500) {
                            revelarUIJogoEProxima()
                        }
                    }
                }
            }
        }
    }

    private fun roteiroPosPergunta1() {
        esconderUIJogo()
        mostrarAssetInterludio("pergunta2", "som5", 1500) { revelarUIJogoEProxima() }
    }

    private fun roteiroPosPergunta2() {
        esconderUIJogo()
        mostrarAssetInterludio("imagem2", "som7", 2000) {
            mostrarAssetInterludio("mediana", "som8", 1500) {
                mostrarAssetInterludio("pergunta3", "som9", 1500) { revelarUIJogoEProxima() }
            }
        }
    }

    private fun roteiroPosPergunta3() {
        esconderUIJogo()
        mostrarAssetInterludio("pergunta4", "som11", 1500) { revelarUIJogoEProxima() }
    }

    private fun roteiroPosPergunta4() {
        esconderUIJogo()
        mostrarAssetInterludio("imagem3", "som13", 4000) {
            mostrarAssetInterludio("hard", "som14", 1500) {
                mostrarAssetInterludio("pergunta5", "som15", 1500) { revelarUIJogoEProxima() }
            }
        }
    }

    private fun esconderUIJogo() {
        findViewById<LinearLayout>(R.id.timerArea).visibility = View.INVISIBLE
        findViewById<TextView>(R.id.txtPergunta).visibility = View.INVISIBLE
        findViewById<LinearLayout>(R.id.layoutOpcoes).visibility = View.INVISIBLE
    }

    private fun revelarUIJogoEProxima() {
        findViewById<LinearLayout>(R.id.timerArea).visibility = View.VISIBLE
        findViewById<TextView>(R.id.txtPergunta).visibility = View.VISIBLE
        findViewById<LinearLayout>(R.id.layoutOpcoes).visibility = View.VISIBLE
        proximaPergunta()
    }


    // Lógica Principal de Quiz e Pontuação
    private fun proximaPergunta() {
        if (indiceAtual >= perguntasLista.size) {
            encerrarPartida()
            return
        }

        respondido = false
        val p = perguntasLista[indiceAtual]
        findViewById<TextView>(R.id.txtPergunta).text = p.texto

        val botoes = listOf<Button>(
            findViewById(R.id.opt0), findViewById(R.id.opt1),
            findViewById(R.id.opt2), findViewById(R.id.opt3)
        )

        val regex = Regex("^[A-Da-d]\\s*[\\)\\.\\-]\\s*")
        val opcoesEmbaralhadas = p.opcoes.mapIndexed { index, texto -> Pair(index, texto.replace(regex, "")) }.shuffled()
        indiceCorretoGlobal = opcoesEmbaralhadas.indexOfFirst { it.first == p.correta }
        val prefixos = listOf("A) ", "B) ", "C) ", "D) ")

        opcoesEmbaralhadas.forEachIndexed { i, par ->
            botoes[i].isClickable = true
            botoes[i].text = prefixos[i] + par.second
            botoes[i].background = criarFundoBotao(isOpcao = true)
            botoes[i].setOnClickListener {
                if (!respondido) validarResposta(i)
            }
        }

        tocarSomDaPergunta(p.audio?.replace(".mp3", ""))
        iniciarTimer(10000)
    }

    private fun iniciarTimer(tempoMs: Long) {
        timer?.cancel()

        timerPlayer?.release()
        val idTimer = resources.getIdentifier("timer", "raw", packageName)
        if (idTimer != 0 && statsManager.somAtivo) {
            timerPlayer = MediaPlayer.create(this, idTimer)
            timerPlayer?.isLooping = true
            timerPlayer?.start()
        }

        val barra = findViewById<ProgressBar>(R.id.progressTimer)
        val textoTimer = findViewById<TextView>(R.id.txtTimer)
        val maxProgress = 10000L

        timer = object : CountDownTimer(tempoMs, 50) {
            override fun onTick(millisUntilFinished: Long) {
                tempoRestanteMs = millisUntilFinished
                textoTimer.text = (millisUntilFinished / 1000 + 1).toString()

                val progressoRelativo = if (millisUntilFinished > maxProgress) 100 else (millisUntilFinished * 100 / maxProgress).toInt()
                barra.progress = progressoRelativo
            }

            override fun onFinish() {
                if (!respondido) validarResposta(-1)
            }
        }.start()
    }

    private fun validarResposta(escolhida: Int) {
        respondido = true
        timer?.cancel()
        pararAudiosAtivos()

        val acertou = escolhida == indiceCorretoGlobal

        val botoes = listOf<Button>(
            findViewById(R.id.opt0), findViewById(R.id.opt1),
            findViewById(R.id.opt2), findViewById(R.id.opt3)
        )

        if (escolhida != -1) {
            if (acertou) {
                botoes[escolhida].background = criarFundoBotao(corOverride = "#34c759")
            } else {
                botoes[escolhida].background = criarFundoBotao(corOverride = "#ff3b30")
                botoes[indiceCorretoGlobal].background = criarFundoBotao(corOverride = "#34c759")
            }
        } else {
            botoes[indiceCorretoGlobal].background = criarFundoBotao(corOverride = "#34c759")
        }

        if (escolhida == -1) {
            tocarSom("semresposta")
        } else if (acertou) {
            tocarSom("acertou")
        } else {
            tocarSom("errou")
        }

        if (acertou) {
            if (tempoRestanteMs > 5000) {
                comboRapido++
                if (comboRapido >= 20) multiplicador = 16
                else if (comboRapido >= 15) multiplicador = 8
                else if (comboRapido >= 10) multiplicador = 4
                else if (comboRapido >= 5) multiplicador = 2
            } else {
                comboRapido = 0
                multiplicador = 1
            }

            if (modoJogo == "sobrevivencia") {
                acertosNaPartida++
                pontuacaoAtual += (1 * multiplicador)
                val txtPontuacao = findViewById<TextView>(R.id.txtPontuacao)
                txtPontuacao.text = "Pontos: $pontuacaoAtual"

                if(multiplicador > 1) {
                    txtPontuacao.setTextColor(Color.parseColor("#ff9500"))
                } else {
                    txtPontuacao.setTextColor(Color.parseColor("#FFD700"))
                }
            }

            checarConquistas()
            statsManager.acertosTotais++

            if (modoJogo == "sobrevivencia" && acertosNaPartida >= 100) {
                mainHandler.postDelayed({ exibirTelaFinal("${statsManager.temaAtual}_ganhou", "ganhou", 8000) }, 2000)
                return
            }
        } else {
            statsManager.errosTotais++
            if (modoJogo == "sobrevivencia") {
                mainHandler.postDelayed({ exibirTelaFinal("${statsManager.temaAtual}_gameover", "gameover", 5000) }, 2000)
                return
            }
        }

        indiceAtual++

        mainHandler.postDelayed({
            if (modoJogo == "padrao") {
                when (indiceAtual) {
                    1 -> roteiroPosPergunta1()
                    2 -> roteiroPosPergunta2()
                    3 -> roteiroPosPergunta3()
                    4 -> roteiroPosPergunta4()
                    else -> proximaPergunta()
                }
            } else {
                proximaPergunta()
            }
        }, 2000)
    }

    private fun checarConquistas() {
        if (tempoRestanteMs > 7000 && !statsManager.temConquista(14)) {
            notificarConquista(14, "Rápido no Gatilho")
        }
        if (comboRapido >= 5 && !statsManager.temConquista(11)) {
            notificarConquista(11, "Olho de Águia")
        }
    }

    @Suppress("DEPRECATION")
    private fun notificarConquista(id: Int, nome: String) {
        statsManager.ganharConquista(id)
        tocarSom("plim")

        val layout = layoutInflater.inflate(R.layout.layout_conquista_toast, null)
        val txtNome = layout.findViewById<TextView>(R.id.txtConquistaNome)
        val imgIcon = layout.findViewById<ImageView>(R.id.imgConquistaIcon)

        txtNome.text = nome
        txtNome.typeface = resources.getFont(R.font.comic)

        val numStr = id.toString().padStart(2, '0')
        val idImg = resources.getIdentifier("badge_$numStr", "drawable", packageName)
        if (idImg != 0) imgIcon.setImageResource(idImg)

        val toast = Toast(applicationContext)
        toast.duration = Toast.LENGTH_LONG
        toast.view = layout
        toast.show()
    }

    private fun encerrarPartida() {
        if (modoJogo == "padrao") {
            exibirTelaFinal("${statsManager.temaAtual}_final", "fim_partida", 8000)
        } else {
            registrarRecordeSeNecessario()
        }
    }

    private fun exibirTelaFinal(imgNome: String, somNome: String, delay: Long) {
        esconderUIJogo()
        val overlay = findViewById<FrameLayout>(R.id.overlayFimJogo)
        val imgFim = findViewById<ImageView>(R.id.imgFimJogo)

        val idImagem = resources.getIdentifier(imgNome, "drawable", packageName)
        if (idImagem != 0) imgFim.setImageResource(idImagem)

        overlay.visibility = View.VISIBLE
        tocarSom(somNome)

        statsManager.partidasJogadas++

        mainHandler.postDelayed({
            if (modoJogo == "sobrevivencia") {
                registrarRecordeSeNecessario()
            } else {
                finish()
            }
        }, delay)
    }


    // Sistema de salvamento de Recorde e Sobrevivência
    private fun registrarRecordeSeNecessario() {
        val top5 = statsManager.getTop5()
        val menorTop5 = if (top5.size == 5) top5.last().pontos else -1

        if (pontuacaoAtual > menorTop5 && pontuacaoAtual > 0) {
            val dialog = Dialog(this)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.background = criarFundoModal()
            layout.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20))

            val titulo = TextView(this)
            titulo.text = "NOVO RECORDE!\nVocê fez $pontuacaoAtual pts!"
            titulo.setTextColor(Color.parseColor(getCorTemaHex()))
            titulo.textSize = 22f
            titulo.textAlignment = View.TEXT_ALIGNMENT_CENTER
            titulo.typeface = resources.getFont(R.font.comic)
            titulo.setPadding(0, 0, 0, dpToPx(20))
            layout.addView(titulo)

            val inputNome = EditText(this)
            inputNome.hint = "Digite seu Nome"
            inputNome.setHintTextColor(Color.GRAY)
            inputNome.setTextColor(Color.WHITE)
            inputNome.typeface = resources.getFont(R.font.comic)
            inputNome.background = criarFundoBotao(isOpcao = true)
            inputNome.setPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15))
            layout.addView(inputNome)

            val espaco = Space(this)
            espaco.layoutParams = LinearLayout.LayoutParams(1, dpToPx(20))
            layout.addView(espaco)

            val btnSalvar = Button(this)
            btnSalvar.text = "Salvar Recorde"
            btnSalvar.background = criarFundoBotao()
            btnSalvar.setTextColor(Color.WHITE)
            btnSalvar.typeface = resources.getFont(R.font.comic)
            btnSalvar.setOnClickListener {
                val nomeDigitado = if (inputNome.text.toString().trim().isEmpty()) "Jogador" else inputNome.text.toString().trim()
                statsManager.salvarRecorde(Recorde(nomeDigitado, pontuacaoAtual))
                dialog.dismiss()
                finish()
            }
            layout.addView(btnSalvar)

            dialog.setCancelable(false)
            dialog.setContentView(layout)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.85).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.show()
        } else {
            finish()
        }
    }

    private fun tocarSomDaPergunta(nomeArquivo: String?) {
        perguntaPlayer?.release()
        if (nomeArquivo.isNullOrEmpty() || !statsManager.somAtivo) return
        val idSom = resources.getIdentifier(nomeArquivo, "raw", packageName)
        if (idSom != 0) {
            perguntaPlayer = MediaPlayer.create(this, idSom)
            perguntaPlayer?.start()
        }
    }

    private fun tocarSom(nomeArquivo: String?) {
        if (nomeArquivo.isNullOrEmpty() || !statsManager.somAtivo) return
        val idSom = resources.getIdentifier(nomeArquivo, "raw", packageName)
        if (idSom != 0) MediaPlayer.create(this, idSom).start()
    }

    private fun pararAudiosAtivos() {
        perguntaPlayer?.stop()
        perguntaPlayer?.release()
        perguntaPlayer = null

        timerPlayer?.stop()
        timerPlayer?.release()
        timerPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        pararAudiosAtivos()
        mainHandler.removeCallbacksAndMessages(null)
    }
}