import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Emisor {

    //Variables globales
    public SourceDataLine emisor;
    protected static final int SAMPLE_RATE = 16 * 1024;
    public String generador = "10001000000100001"; //Generador del CRC
    
    //Funcion para pasar la cadena a1 binario
    public String pasar_a_binario(String palabra, String cadena)
    {
        int i, cont;
       
        for(i = 0; i < palabra.length(); i++)
        {
            cont = 0;
                
            StringBuilder builder = new StringBuilder(transformarCaracteres((int)(palabra.charAt(i)),cont));    
            cadena = cadena + builder.reverse().toString();
          
        }
        
        return cadena;
    }
    
    //Funcion para transformar el numero de la tabla ascci a binario
    public String transformarCaracteres(int letra, int cont)
    {
        String aux = "";
        while (letra > 0)
        {
            cont = cont+1;
            aux = aux + Integer.toString(letra%2);
            letra = letra/2;            
        }
        
        if(cont < 8)
            aux = rellenarConCeros(aux,cont);
        
        return aux;
    }
    
    //Funcion para rellenar con 0´s la cadena en caso de que no sea multiplo de 8
    public String rellenarConCeros(String cadena, int cont)
    {
        while (cont < 8)
        {
            cadena = cadena + "0";
            cont = cont+1;
        }
        return cadena;
    }

    //Funcion que agrega los bits de control del CRC a la cadena
    public String CRC_Emisor(String bits)
    {
        String residuo = bits, residuoaux = "", aux;
        int i, j = 0, k, longitudgene = generador.length()-1;
        boolean flag = true;

        while(j < generador.length()-1)
        {
        residuo = residuo + "0";
        j++;   
        }   
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
                residuoaux = "";
            }       
        }
        }
       
        residuoaux = "";
        for(k = 0; k <= residuo.length()-1; k++)
        {
        if(residuo.charAt(k) != aux.charAt(k))
            residuoaux = residuoaux + "1";
        else
            residuoaux = residuoaux + "0";
        }

        return residuoaux;
    }

    //Funcion que aplica el relleno de bits a la cadena
    public String RellenodeBits_Emisor(String bits)
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
              aux = aux+"0";
              bits = aux+bits.substring(i+1,bits.length());
              longitud++;
          }   
        }
        return bits;
    }

    //Funcion que le agrega las banderas de inicio y fin a la cadena 
    public String Bandera_Emisor(String bits)
    {
        return "11111111"+bits+"11111111";
    }
    
    //Constructor del Emisor
    public Emisor(String palabra) throws LineUnavailableException, InterruptedException {
        
        String cadena ="";
        int j = 0, c = 0, i;
       
       
	cadena = pasar_a_binario (palabra,cadena);
        cadena = CRC_Emisor(cadena);
        cadena = RellenodeBits_Emisor(cadena);
        cadena = Bandera_Emisor(cadena);
        System.out.println(cadena);
	//Bandera para saber si la palabra a enviar proviene del usuario o de la retransmision 
        MainMenu.flagmain = true;
	//Guarda la palabra a retransmitir
        MainMenu.palabrare = palabra;

	//Crea el vector de bytes con la longitud necesaria para enviar la cadena
        byte[] trama = new byte[palabra.length() * 8 * 12 + 5000];
        //Ciclo para llenar el vector de bytes a escribir en el buffer
	do{
            if(cadena.charAt(j) == '1')
            {
                for(int a=0;a<4;a++)
                {
                    trama[c] = 100;
                    c++;
                }
                for(int a=0;a<8;a++)
                {
                    trama[c] = 0;
                    c++;
                }
            }
            else
            {
                for(int a=0;a<4;a++)
                {
                    trama[c] = -127;
                    c++;
                }
                for(int a=0;a<8;a++)
                {
                    trama[c] = 0;
                    c++;
                }
            }
            j++;
        }while(j < cadena.length());

        //Codigo donde se inicializan los parametros para escribir los bytes en el buffer
	AudioFormat formato_de_la_señal = new AudioFormat((float) (SAMPLE_RATE),16,1,true,true);         
        emisor = AudioSystem.getSourceDataLine(formato_de_la_señal);
        emisor.open(formato_de_la_señal,SAMPLE_RATE);
        emisor.start();
        
	//Seccion de codigo donde se calcula el tiempo de transmision
        long time_start, time_end;
        time_start = System.currentTimeMillis();
        emisor.write(trama, 0, trama.length);
        time_end = System.currentTimeMillis();
        
	//Se escribe el tiempo de transmision
        System.out.println("Palabra enviada: " + palabra + "\nLongitud de la palabra: " + palabra.length() + "\nBits transmitidos: " + cadena.length() + "\nTiempo de transmision: "+ ( time_end - time_start ) +" millisegundos");
        emisor.close();
    } 
  }
