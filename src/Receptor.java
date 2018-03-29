//Librerias
import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JTextArea;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Receptor extends Thread
{

    //Variables globales
    public String generador = "10001000000100001"; //Generador del CRC

    //Constructor del receptor donde se llama el hilo del receptor
    public Receptor(final JTextArea PanelMensajes) throws LineUnavailableException
    {
       	//Parametros de inicializacion de la tarjeta de sonido
	final ByteArrayOutputStream out = new ByteArrayOutputStream();
        float sampleRate = 16 * 1024;
        int sampleSizeInBits = 8;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        final AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
                bigEndian);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

	//Hilo que ejecuta el receptor
        Runnable runner;
        runner = new Runnable()
        {
	    //Variables globales del hilo
            int bufferSize = (int) format.getSampleRate() * format.getFrameSize();
            int count = 0,i;

	    //Funcion para pasar la cadena de binario a caracter            
	    public String  pasar_a_caracter (String cadena, int longitud)
            {
                int i = 0, k, num, cont = 0;
                String palabra="";
                do
                {
                    num = 0;
                    for(k = 7; k >= 0; k--)
                    {
                        if(cadena.charAt(i) == '1')
                            num = (int) (num+Math.pow(2,k));
                        i++;
                        longitud--;
                    }
                    palabra = palabra + (char)(num);
                    cont++;
                }
                while(longitud > 0);

                return palabra;
            }

	    //Quita los digitos que ya paso a caracter
            public String QuitarDigitos(String cadena, int longitud)
            {
                cadena = cadena.substring(longitud,cadena.length());

                return cadena;
            }

	    //Realiza la division entre el generador y verifica si da 0 si es asi quita los bits de control del CRC
            public String CRC_Receptor(String bits)
            {
                String residuo = bits, residuoaux = "", aux;
                int i, j = 0, k, longitudgene = generador.length()-1,es_divisible = 0;
                boolean flag = true;

                aux = residuo;
                i = residuo.length();

                while(j < i && flag)
                {
                    if(j < i && flag)
                    {
                        j = 0;
                        while(j < i && residuo.charAt(j) != '1')
                        {
                            j++;
                            residuoaux = residuoaux + "0";
                        }

                        if(j + longitudgene+1 > i)
                            flag = false;

                        if(flag)
                        {
                            for(k = 0; k <= longitudgene; k++)
                            {
                                if(residuo.charAt(j) != generador.charAt(k))
                                    residuoaux = residuoaux + "1";
                                else
                                    residuoaux = residuoaux + "0";
                                j++;
                            }

                            residuoaux = residuoaux+residuo.substring(j,residuo.length());
                            residuo = residuoaux;
                            es_divisible = residuoaux.indexOf('1');
                            residuoaux = "";
                        }
                    }
                }

                if(-1 == es_divisible)
                    bits = bits.substring(0,bits.length()-16);

                return bits;
            }

	    //Desace el relleno de bits de la trama
            public String RellenodeBits_Receptor(String bits)
            {
                String aux;
                int i,cont = 0,longitud = bits.length()-1;

                for(i = 0; i <= longitud; i++)
                {
                    if(bits.charAt(i) == '1')
                        cont++;
                    else
                        cont = 0;

                    if(cont == 5)
                    {
                        aux = bits.substring(0,i+1);
                        bits = aux+bits.substring(i+2,bits.length());
                        longitud--;
                    }
                }
                return bits;
            }

     	    //Quita la bandera de inicio y fin de la trama
	     public String Bandera_Receptor(String bits)
            {
                return bits.substring(8,bits.length()-8);
            }


	
	//Detecta la bandera del final dela trama
        public boolean Detectar_bandera(String bits)
        {
            int cont = 0;
        for(int i = bits.length()-1; i >= bits.length()-8; i--)
        {
            if(bits.charAt(i) == '1')
                cont++;
                }

        return cont == 8;
        }
    
	//Realiza la retransmision en caso de que la trama llegue mal
        private void Retransmitir() 
        {
               Date date = new Date();
               DateFormat hourdateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        
        if(MainMenu.palabrare != "")
        {
                try {
                   Emisor p = new Emisor(MainMenu.palabrare);
                } catch (LineUnavailableException ex) {
                      Logger.getLogger(MainMenu.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainMenu.class.getName()).log(Level.SEVERE, null, ex);
                }
        }
           }

	    //Funcion donde se ejecuta el receptor
            public void run()
            {
                String cadena="",palabra="",aux="";
                boolean flag = false, flagB,flagc;
	  	int cont = 0;

		//Ejecuta el receptor durante toda la ejecucion del programa
                while (true)
                {

                    byte buffer[] = new byte[bufferSize];
                    count = line.read(buffer, 0, buffer.length);
                    flagB = false;
                   flagc = false;
		
		    //Ciclo donde se revisan los valores leidos del buffer y se va armanado la trama segun lo leido
                    for(i = 0; i < bufferSize; i++)
                    {
                        if(buffer[i] >= -90 && buffer[i] <= 90)
                        {
                            flag = false;
                        }

                        if(buffer[i] > 110 && !flag)
                        {
                            cadena += '1';
                            flag = true;
                            flagB = true;

                        }
                        else if(buffer[i] < -110 && !flag)
                        {
                            cadena += '0';
                            flag = true;
                            flagB = true;
                        }

  
                    }

		    //Condicional donde se le aplica el CRC, Relleno de bits y se le remueve la bandera cuando la trama se armo por completo
                    if(!flagB && cadena != "" && Detectar_bandera(cadena))
                    {
                        cadena = Bandera_Receptor(cadena);
                        cadena = RellenodeBits_Receptor(cadena);
                        aux = CRC_Receptor(cadena);
                        //Condicional para determinar si debe existir retransmision o no
			if(cadena.length() > aux.length())
                        {
                            //Ciclo para armar el mensaje recibido
			    while(aux.length() >= 8)
                            {
                                palabra = palabra+pasar_a_caracter(aux,8);
                                aux = QuitarDigitos(aux,8);
                            }
                        }
                        else
            {
                            System.out.println("Retransmsion");
                Retransmitir();
            }                        
            cadena = "";
                    }

		    //Se muestra el mensaje recibido en la interfaz 
                    if(!"".equals(palabra) && aux.length() == 0)
                    {
                        Date date = new Date();
                        DateFormat hourdateFormat = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

                        PanelMensajes.setText(PanelMensajes.getText() + "[RECIBIDO]" + "[" + hourdateFormat.format(date) + "]: " + palabra + "\n");
                        palabra = "";
            flagc = true;
            MainMenu.flagmain = false;
                    }
            cont++;

	    //Ciclo para saber si el temporizador expiro y hay que retransmitir
            if(cont > 3 && !flagc && MainMenu.flagmain)
            {
             System.out.println("Retransmsion");
             Retransmitir();
             cont = 0;
            }
                }
            }
        };
        Thread captureThread = new Thread(runner);
        captureThread.start();
    }
}
