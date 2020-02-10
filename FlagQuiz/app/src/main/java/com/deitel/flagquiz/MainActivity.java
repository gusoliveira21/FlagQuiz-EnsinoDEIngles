// MainActivity.java
// Hosts the MainActivityFragment on a phone and both the
// MainActivityFragment and SettingsActivityFragment on a tablet
package com.deitel.flagquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
   // chaves para ler dados de SharedPreferences
   public static final String ESCOLHAS = "pref_numberOfChoices";
   public static final String REGIOES = "pref_regionsToInclude";

   private boolean dispositivoCelular = true; // usado para forçar o modo retrato
   private boolean preferencasMudadas = true; // as preferências mudaram?

   // configura o MainActivity
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);

      /* define valores padrão nas SharedPreferences do aplicativo
      As SharedPreferences consiste em uma interface que permite acessar e modificar dados de
      preferência de usuário. O valor armazenado apresenta-se sob formato chave-valor ou key-value,
      ou seja, cada preferência armazenada possui uma identificação ou chave e associada a ela está
      um valor. Ela permite armazenamento de diversos tipos de valor, como int, float, Strings,
      booleans e sets de Strings.
      */
      PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

      // registra ouvinte para alterações de SharedPreferences
      PreferenceManager.getDefaultSharedPreferences(this).
         registerOnSharedPreferenceChangeListener(
            mudancaDePreferencia_ouvinte);

      // determina o tamanho da tela
      int screenSize = getResources().getConfiguration().screenLayout &
         Configuration.SCREENLAYOUT_SIZE_MASK;

      // se o dispositivo for um tablet, defina dispositivoCelular como false
      if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
         screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE)
         dispositivoCelular = false; // não é um dispositivo do tamanho de um telefone

      // se estiver sendo executado em um dispositivo do tamanho de um telefone, permita apenas a orientação retrato
      if (dispositivoCelular)
         setRequestedOrientation(
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
   }

   // chamado após o onCreate concluir a execução
   @Override
   protected void onStart() {
      super.onStart();

      if (preferencasMudadas) {
         /*
         agora que as preferências padrão foram definidas,
         inicialize MainActivityFragment e inicie o questionário
         */
         MainActivityFragment quizFragment = (MainActivityFragment)
            getSupportFragmentManager().findFragmentById(
               R.id.quizFragment);
         quizFragment.updateGuessRows(
            PreferenceManager.getDefaultSharedPreferences(this));
         quizFragment.updateRegions(
            PreferenceManager.getDefaultSharedPreferences(this));
         quizFragment.resetQuiz();
         preferencasMudadas = false;
      }
   }

   // mostra o menu se o aplicativo estiver sendo executado em um telefone ou tablet orientado a retrato
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // obtém a orientação atual do dispositivo
      int orientation = getResources().getConfiguration().orientation;

      // exibe o menu do aplicativo apenas na orientação retrato
      if (orientation == Configuration.ORIENTATION_PORTRAIT) {

      // inflar o menu
         getMenuInflater().inflate(R.menu.menu_main, menu);
         return true;
      }
      else
         return false;
   }

   // exibe o SettingsActivity ao executar em um telefone
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      Intent intentDePreferencia = new Intent(this, SettingsActivity.class);
      startActivity(intentDePreferencia);
      return super.onOptionsItemSelected(item);
   }

   // listener para alterações nas SharedPreferences do aplicativo
   private OnSharedPreferenceChangeListener mudancaDePreferencia_ouvinte =
      new OnSharedPreferenceChangeListener() {

         // chamado quando o usuário altera as preferências do aplicativo
         @Override
         public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
            preferencasMudadas = true; // configuração de aplicativo alterada pelo usuário

            MainActivityFragment quizFragment = (MainActivityFragment)
               getSupportFragmentManager().findFragmentById(
                  R.id.quizFragment);

            if (key.equals(ESCOLHAS)) { // Nº de opções a serem exibidas alteradas
               quizFragment.updateGuessRows(sharedPreferences);//Troca o numero de tentativas
               quizFragment.resetQuiz();
            }
            else if (key.equals(REGIOES)) { // regiões para incluir alterações
               Set<String> regions =
                  sharedPreferences.getStringSet(REGIOES, null);

               if (regions != null && regions.size() > 0) {
                  quizFragment.updateRegions(sharedPreferences);
                  quizFragment.resetQuiz();
               }
               else {
                  // deve selecionar uma região - defina a América do Norte como padrão
                  SharedPreferences.Editor editor =
                     sharedPreferences.edit();
                  regions.add(getString(R.string.default_region));
                  editor.putStringSet(REGIOES, regions);
                  editor.apply();

                  Toast.makeText(MainActivity.this,
                     R.string.default_region_message,
                     Toast.LENGTH_SHORT).show();
               }
            }

            Toast.makeText(MainActivity.this,
               R.string.restarting_quiz,
               Toast.LENGTH_SHORT).show();
         }
      };
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
