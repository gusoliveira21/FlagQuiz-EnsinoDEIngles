// MainActivityFragment.java
// Contains the Flag Quiz logic
package com.deitel.flagquiz;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivityFragment extends Fragment {
    // String usada ao registrar mensagens de erro
   private static final String TAG = "FlagQuiz Activity";

   private static final int FLAGS_IN_QUIZ = 10;

   private List<String> nomeListaArquivos; // nomes de arquivo de sinalização
   private List<String> quizListaPaizes; // países no questionário atual
   private Set<String> listaRegioes; // regiões do mundo no questionário atual
   private String respostasCorretas; // país correto para a bandeira atual
   private int totalSuposicoes; // número de suposições feitas
   private int respostasCorretass; // número de suposições corretas
   private int linhasPalpite; // número de linhas exibindo palpite Botões
   private SecureRandom aleatorio; // usado para aleatorioizar o questionário
   private Handler atrazar; // usado para atrasar o carregamento do próximo sinalizador
   private Animation animacao_balancar; // animação para palpites incorretos

   private LinearLayout quizLinearLayout; // layout que contém o questionário
   private TextView questionNumberTextView; //mostra a pergunta atual #
   private ImageView flagImageView; // displays a flag
   private LinearLayout[] guessLinearLayouts; // linhas de resposta Botões
   private TextView answerTextView; // exibe a resposta correta

   // configura o MainActivityFragment quando sua View é criada
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
      super.onCreateView(inflater, container, savedInstanceState);
      View view =
         inflater.inflate(R.layout.fragment_main, container, false);

      nomeListaArquivos = new ArrayList<>();
      quizListaPaizes = new ArrayList<>();
      aleatorio = new SecureRandom();
      atrazar = new Handler();

      //carregue a animação de agitação usada para respostas incorretas
      animacao_balancar = AnimationUtils.loadAnimation(getActivity(),
         R.anim.animacao_incorreta);
      animacao_balancar.setRepeatCount(300); // carregue a animação de agitação usada para respostas incorretas

      // obter referências aos componentes da GUI
      quizLinearLayout =
          view.findViewById(R.id.quizLinearLayout);
      questionNumberTextView =
          view.findViewById(R.id.questionNumberTextView);
      flagImageView =  view.findViewById(R.id.flagImageView);
      guessLinearLayouts = new LinearLayout[4];
      guessLinearLayouts[0] =
          view.findViewById(R.id.row1LinearLayout);
      guessLinearLayouts[1] =
          view.findViewById(R.id.row2LinearLayout);
      guessLinearLayouts[2] =
          view.findViewById(R.id.row3LinearLayout);
      guessLinearLayouts[3] =
          view.findViewById(R.id.row4LinearLayout);
      answerTextView =  view.findViewById(R.id.answerTextView);

      // configurar ouvintes para o palpite
      for (LinearLayout linha : guessLinearLayouts) {
         for (int column = 0; column < linha.getChildCount(); column++) {
            Button button = (Button) linha.getChildAt(column);
            button.setOnClickListener(guessButtonListener);
         }
      }

      // definir o texto de questionNumberTextView
      questionNumberTextView.setText(
         getString(R.string.question, 1, FLAGS_IN_QUIZ));
      return view; // retornar a exibição do fragmento para exibição
   }

   // atualizar linhasPalpite com base no valor em SharedPreferences
   public void updateGuessRows(SharedPreferences sharedPreferences) {
      // obtenha o número de botões de palpite que devem ser exibidos
      String escolhas =
         sharedPreferences.getString(MainActivity.ESCOLHAS, null);
      linhasPalpite = Integer.parseInt(escolhas) / 2;

      // ocultar todos os botões de busca LinearLayouts
      for (LinearLayout layout : guessLinearLayouts)
         layout.setVisibility(View.GONE);

      // exibir botão de palpite apropriado LinearLayouts
      for (int linha = 0; linha < linhasPalpite; linha++)
         guessLinearLayouts[linha].setVisibility(View.VISIBLE);
   }

   // atualizar regiões do mundo para questionários com base em valores em SharedPreferences
   public void updateRegions(SharedPreferences sharedPreferences) {
      listaRegioes =
         sharedPreferences.getStringSet(MainActivity.REGIOES, null);
   }

   // configurar e iniciar o próximo questionário
   public void resetQuiz() {
      //  use o AssetManager para obter nomes de arquivos de imagem para regiões ativadas
      AssetManager assets = getActivity().getAssets();
      nomeListaArquivos.clear(); // lista vazia de nomes de arquivos de imagem

      try {
         // loop através de cada região
         for (String region : listaRegioes) {
            // obtenha uma lista de todos os arquivos de imagem de sinalizadores nesta região
            String[] caminhos = assets.list(region);

            for (String caminho : caminhos)
               nomeListaArquivos.add(caminho.replace(".png", ""));
         }
      }
      catch (IOException exception) {
         Log.e(TAG, "Erro ao carregar nomes de arquivo de imagem", exception);
      }

      respostasCorretass = 0; //redefinir o número de respostas corretas feitas
      totalSuposicoes = 0; // redefinir o número total de suposições feitas pelo usuário
      quizListaPaizes.clear(); // lista prévia clara de países do questionário

      int flagCounter = 1;
      int numberOfFlags = nomeListaArquivos.size();

      // adicione nomes de arquivo aleatórios FLAGS_IN_QUIZ ao quizListaPaizes
      while (flagCounter <= FLAGS_IN_QUIZ) {
         int aleatorioIndice = aleatorio.nextInt(numberOfFlags);

         // obter o nome do arquivo aleatório
         String filename = nomeListaArquivos.get(aleatorioIndice);

         // se a região estiver ativada e ainda não tiver sido escolhida
         if (!quizListaPaizes.contains(filename)) {
            quizListaPaizes.add(filename); // adicione o arquivo à lista
            ++flagCounter;
         }
      }

      loadNextFlag(); // inicie o teste carregando a primeira bandeira
   }

   // depois que o usuário adivinha um sinalizador correto, carregue o próximo sinalizador
   private void loadNextFlag() {
      // obter o nome do arquivo do próximo sinalizador e removê-lo da lista
      String nextImage = quizListaPaizes.remove(0);
      respostasCorretas = nextImage; // atualize a resposta correta
      answerTextView.setText(""); //limpar answerTextView

      // display current question number
      questionNumberTextView.setText(getString(
         R.string.question, (respostasCorretass + 1), FLAGS_IN_QUIZ));

      // extrair a região do nome da próxima imagem
      String region = nextImage.substring(0, nextImage.indexOf('-'));

      // use o AssetManager para carregar a próxima imagem da pasta de ativos
      AssetManager assets = getActivity().getAssets();

      // obter um InputStream para o ativo que representa o próximo sinalizador
      // e tente usar o InputStream
      try (InputStream stream =
              assets.open(region + "/" + nextImage + ".png")) {
         // carregar o ativo como um Drawable e exibir no flagImageView
         Drawable flag = Drawable.createFromStream(stream, nextImage);
         flagImageView.setImageDrawable(flag);

         animate(false); // animar a bandeira na tela
      }
      catch (IOException exception) {
         Log.e(TAG, "Erro a carregar " + nextImage, exception);
      }

      Collections.shuffle(nomeListaArquivos); // embaralhar nomes de arquivo

      // coloque a resposta correta no final de nomeListaArquivos
      int correct = nomeListaArquivos.indexOf(respostasCorretas);
      nomeListaArquivos.add(nomeListaArquivos.remove(correct));

      // adicione 2, 4, 6 ou 8 palpites Botões com base no valor de palpites
      for (int linha = 0; linha < linhasPalpite; linha++) {
         // Coloque os botões no currentTableRow
         for (int column = 0;
              column < guessLinearLayouts[linha].getChildCount();
              column++) {
            // obtenha referência ao Button para configurar
            Button newGuessButton =
               (Button) guessLinearLayouts[linha].getChildAt(column);
            newGuessButton.setEnabled(true);

            // obter o nome do país e defini-lo como o texto de newGuessButton
            String filename = nomeListaArquivos.get((linha * 2) + column);
            newGuessButton.setText(getCountryName(filename));
         }
      }

      // substitua aleatoriamente um botão pela resposta correta
      int linha = aleatorio.nextInt(linhasPalpite); // escolher linha aleatória
      int column = aleatorio.nextInt(2); // escolher coluna aleatória
      LinearLayout aleatorioRow = guessLinearLayouts[linha]; // pegue a linha
      String countryName = getCountryName(respostasCorretas);
      ((Button) aleatorioRow.getChildAt(column)).setText(countryName);
   }

   // analisa o nome do arquivo de bandeira do país e retorna o nome do país
   private String getCountryName(String name) {
      return name.substring(name.indexOf('-') + 1).replace('_', ' ');
   }

   // anima o quizLinearLayout inteiro na tela ou fora dela
   private void animate(boolean animateOut) {
      // impedir animação na interface do usuário para o primeiro sinalizador
      if (respostasCorretass == 0)
         return;

      // calcular centro xe centro y
      int centerX = (quizLinearLayout.getLeft() +
         quizLinearLayout.getRight()) / 2; // calcular centro x
      int centerY = (quizLinearLayout.getTop() +
         quizLinearLayout.getBottom()) / 2; // calcular centro y

      // calcular raio de animação
      int radius = Math.max(quizLinearLayout.getWidth(),
         quizLinearLayout.getHeight());

      Animator animator;

      // se o quizLinearLayout for animado em vez de em
      if (animateOut) {
         // criar animação circular de revelação
         animator = ViewAnimationUtils.createCircularReveal(
            quizLinearLayout, centerX, centerY, radius, 0);
         animator.addListener(
            new AnimatorListenerAdapter() {
               // chamado quando a animação termina
               @Override
               public void onAnimationEnd(Animator animation) {
                  loadNextFlag();
               }
            }
         );
      }
      else { // se o quizLinearLayout deve animar em
         animator = ViewAnimationUtils.createCircularReveal(
            quizLinearLayout, centerX, centerY, 0, radius);
      }

      animator.setDuration(500); // defina a duração da animação para 500 ms
      animator.start(); //comece a animação
   }

   // chamado quando um botão de palpite é tocado
   private OnClickListener guessButtonListener = new OnClickListener() {
      @Override
      public void onClick(View v) {
         Button guessButton = ((Button) v);
         String guess = guessButton.getText().toString();
         String answer = getCountryName(respostasCorretas);
         ++totalSuposicoes; // aumentar o número de suposições que o usuário fez

         if (guess.equals(answer)) { // se o palpite estiver correto
            ++respostasCorretass; // incrementar o número de respostas corretas

            /* exibir resposta correta em texto verde
            answerTextView.setText(answer + "!");
               answerTextView.setTextColor
               (
                  getResources().getColor(R.color.correct_answer, getContext().getTheme())
               );*/

            //MODIFICADO exibir resposta correta em texto verde
            answerTextView.setText("Acertou!");
              answerTextView.setTextColor
               (
                  getResources().getColor(R.color.correct_answer, getContext().getTheme())
               );



            disableButtons(); // exibir resposta correta em texto verde





            // se o usuário tiver identificado corretamente sinalizadores FLAGS_IN_QUIZ
            if (respostasCorretass == FLAGS_IN_QUIZ) {
               // DialogFragment para exibir estatísticas do questionário e iniciar um novo questionário
               DialogFragment quizResults =
                  new DialogFragment() {
                     // crie um AlertDialog e retorne-o
                     @Override
                     public Dialog onCreateDialog(Bundle bundle) {
                        AlertDialog.Builder builder =
                           new AlertDialog.Builder(getActivity());
                        builder.setMessage(
                           getString(R.string.results,
                              totalSuposicoes,
                              (1000 / (double) totalSuposicoes)));

                        // "Resetar Questão" Button
                        builder.setPositiveButton(R.string.reset_quiz,
                           new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog,
                                 int id) {
                                 resetQuiz();
                              }
                           }
                        );

                        return builder.create(); // retorna o AlertDialog
                     }
                  };




               //use FragmentManager para exibir o DialogFragment
               quizResults.setCancelable(false);
               quizResults.show(getFragmentManager(), "quiz results");
            }
            else { /* resposta está correta, mas o teste ainda não termino
               // carrega o próximo sinalizador após um atraso de 2 segundos
                */
              atrazar.postDelayed(
                  new Runnable() {
                     @Override
                     public void run() {
                        animate(true); // animar a bandeira da tela
                     }
                  }, 2000); //2000 milissegundos para atraso de 2 segundos
            }
         }
         else { // resposta incorreta
            flagImageView.startAnimation(animacao_balancar); //jogar shake

            // display "Incorreto!" in red
            answerTextView.setText(R.string.incorrect_answer);
            answerTextView.setTextColor(getResources().getColor(
               R.color.incorrect_answer, getContext().getTheme()));
            guessButton.setEnabled(false); // desativar resposta incorreta
         }
      }
   };

   // método utilitário que desativa todas as respostas
   private void disableButtons() {
      for (int linha = 0; linha < linhasPalpite; linha++) {
         LinearLayout guessRow = guessLinearLayouts[linha];
         for (int i = 0; i < guessRow.getChildCount(); i++)
            guessRow.getChildAt(i).setEnabled(false);
      }
   }
}


/*************************************************************************
 * (C) Copyright 1992-2016 by Deitel & Associates, Inc. and               *
 * Pearson Education, Inc. All Rights Reserved.                           *
 *                                                                        *
 * DISCLAIMER: The authors and publisher of this book have used their     *
 * best efforts in preparing the book. These efforts include the          *
 * development, research, and testing of the theories and programs        *
 * to determine their effectiveness. The authors and publisher make       *
 * no warranty of any kind, expressed or implied, with regard to these    *
 * programs or to the documentation contained in these books. The authors *
 * and publisher shall not be liable in any event for incidental or       *
 * consequential damages in connection with, or arising out of, the       *
 * furnishing, performance, or use of these programs.                     *
 *************************************************************************/
