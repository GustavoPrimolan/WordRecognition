package main;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.io.File;
import java.util.List;
import recognizer.CharacterRecognizer;

public class Main {
    
    public static void main(String[] args) {
        
        File imagem = new File("imagensSeparadas/numeroConta.png");
        CharacterRecognizer cr = new CharacterRecognizer(imagem);
        String imagemBinaria = "imagensSeparadas/numeroContaBinario.png";
        //NECESSÁRIO SEMPRE FAZER A BINARIAZAÇÃO DA IMAGEM
        cr.binarizaImagem(imagemBinaria, 155);
        
        File imgBinaria = new File(imagemBinaria);
        //cr.imagemJusta(imgBinaria);
        String texto = cr.recognize(imgBinaria);
        
        System.out.println("Texto reconhecido: " + texto);
       
        
    }
}
