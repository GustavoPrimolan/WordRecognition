package recognizer;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.ContrastEnhancer;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CharacterRecognizer {

    private File imagemOriginal;
    private int qtdCarac = 0;
    private File dirProjeto = null;
    private File dirImagemBinaria = null;
    private File dirBaseImagens = null;
    private File dirImagensCortadas = null;

    public int getQtdCarac() {
        return this.qtdCarac;
    }

    public CharacterRecognizer(File imagemOriginal) {
        this.dirProjeto = new File("C:\\WordRecognition");
        this.dirImagemBinaria = new File(dirProjeto.getAbsolutePath() + "\\ImagemBinaria");
        this.dirBaseImagens = new File(dirProjeto.getAbsolutePath() + "\\BancoImagens");
        this.dirImagensCortadas = new File(dirProjeto.getAbsolutePath() + "\\ImagensCortadas");
        if (!dirProjeto.exists()) {
            dirProjeto.mkdir();
        }

        if (!dirImagemBinaria.exists()) {
            dirImagemBinaria.mkdir();
        }

        if (!dirBaseImagens.exists()) {
            dirBaseImagens.mkdir();
        }

        if (!dirImagensCortadas.exists()) {
            dirImagensCortadas.mkdir();
        }

        this.imagemOriginal = imagemOriginal;

    }

    //MÉTODO COM O INTUITO DE BINARIZAR A IMAGEM
    private File binarizaImagem(int limiar) {
        //INSTANCIA A IMAGEM
        ImagePlus img = ij.IJ.openImage(imagemOriginal.getAbsolutePath());

        //ABRE O CONVERTER PARA CONVERTER PARA ESCALA DE CINZA EM 8 BITS
        ImageConverter ic = new ImageConverter(img);
        //CONVERTE PARA 8 BITS
        ic.convertToGray8();
        //ATUALIZA E DESENHA A IMAGEM
        img.updateAndDraw();

        ImageProcessor ip = img.getProcessor();

        //BINARIZA A IMAGEM
        for (int i = 0; i < ip.getWidth(); i++) {
            for (int j = 0; j < ip.getHeight(); j++) {
                if (ip.getPixel(i, j) > limiar) {
                    ip.putPixel(i, j, 255);
                } else {
                    ip.putPixel(i, j, 0);
                }
            }
        }

        File imagemBinaria = new File(dirImagemBinaria.getAbsolutePath() + "\\Binaria-" + imagemOriginal.getName());

        //O CAMINHO DEVERÁ SER SALVO JUNTO COM O NOME DO ARQUIVO
        IJ.save(img, imagemBinaria.getAbsolutePath());

        return imagemBinaria;
    }

    private ImagePlus imagemJusta(File imagemBinaria) {
        int alturaMax = 0, larguraMax = 0, alturaMin = 0, larguraMin = 0;
        ImagePlus imgBinaria = ij.IJ.openImage(imagemBinaria.getAbsolutePath());
        ImageProcessor ipBinaria = imgBinaria.getProcessor();

        boolean inicializouMinimos = false;
        //SEPARA TUDO EM IMAGEM MENOR DE TAMANHO JUSTO AOS CARACTERES
        for (int i = 0; i < ipBinaria.getWidth(); i++) {
            for (int j = 0; j < ipBinaria.getHeight(); j++) {
                //SE O PIXEL FOR PRETO
                //COMPARA A ALTURA E A LARGURA PARA VER AS PROPORÇÕES DA IMAGEM
                if (ipBinaria.getPixel(i, j) == 0) {
                    //É NECESSÁRIO INICIALIZAR AS VARIAVEIS COM ALGUM VALOR PARA CONSEGUIR COMPARAR
                    //POIS SE EU INICIALIZAR COM O VALOR 0, NÃO HAVERA NENHUMA POSIÇÃO MENOR
                    //QUE 0
                    if (!inicializouMinimos) {
                        larguraMin = i;
                        alturaMin = j;
                        inicializouMinimos = true;
                    }

                    if (i > larguraMax) {
                        larguraMax = i;
                    } else if (i < larguraMin) {
                        larguraMin = i;
                    }

                    if (j > alturaMax) {
                        alturaMax = j;
                    } else if (j < alturaMin) {
                        alturaMin = j;
                    }
                }
            }
        }

        //System.out.println("Altura máxima da imagem justa: " + alturaMax);
        //System.out.println("Largura máxima da imagem jsuta: " + larguraMax);
        //System.out.println("Altura mínima da imagem justa: " + alturaMin);
        //System.out.println("Largura mínima da imagem justa: " + larguraMin);
        //É NECESSÁRIO SOMAR MAIS 1 NA ALTURA MAX E NA LARGURA MAX POR CONTA DA DIFERENÇA
        alturaMax = alturaMax + 1;
        larguraMax = larguraMax + 1;
        int alturaJusta = alturaMax - alturaMin;
        int larguraJusta = larguraMax - larguraMin;
        //System.out.println("Proporção da imagem justa:\nLargura: " + larguraJusta + "\nAltura: " + alturaJusta);
        //System.out.println("Gerando imagem justa...");

        //CRIA A IMAGEM COM O TAMANHO EXATAMENTE DOS CARACTERES
        ImagePlus imagemJusta = ij.IJ.createImage("ImagemJusta", "png", larguraJusta, alturaJusta, 8);

        String dirImagemJusta = dirImagemBinaria.getAbsolutePath() + "\\ImagemJusta-" + imagemOriginal.getName();
        ImageProcessor ipJusta = imagemJusta.getProcessor();

        //JOGA OS PIXELS DA IMAGEM BINARIA PARA A IMAGEM JUSTA
        //NA MESMA PROPORÇÃO
        for (int i = larguraMin; i < larguraMax; i++) {
            for (int j = alturaMin; j < alturaMax; j++) {
                int pixelBinaria = ipBinaria.getPixel(i, j);
                //PARA QUE SEJA PROPORCINAL É NECESSÁRIO SUBTRAIR A i COM O larguraMin
                //E SUBTRAIR j ALTURA MÁXIMA COM O j
                ipJusta.putPixel(i - larguraMin, j - alturaMin, pixelBinaria);
            }
        }

        IJ.save(imagemJusta, dirImagemJusta);
        //System.out.println("Imagem justa gerada: " + dirImagemJusta);
        return imagemJusta;
    }

    //MÉTODO QUE RETORNA A IMAGEM BINARIAZADA SEPARADA EM VÁRIAS IMAGENS
    private List<ImagePlus> imagensSeparadas(ImagePlus imgJusta) {

        List<ImagePlus> slicedImages = new ArrayList<>();
        ImageProcessor ipJusta = imgJusta.getProcessor();

        boolean encontrouPixelPreto = false;
        boolean encontrouCarac = false;
        boolean ultimoCarac = false;
        boolean primeiroCarac = false;
        int carac = 1;
        int larguraCaracAnt = 0;
        for (int i = 0; i < imgJusta.getWidth(); i++) {
            for (int j = 0; j < imgJusta.getHeight(); j++) {
                //VERIFICA SE TEM PIXEL PRETO NA COLUNA
                if (ipJusta.getPixel(i, j) == 0) {
                    encontrouPixelPreto = true;
                    encontrouCarac = true;
                }
                if (i == (imgJusta.getWidth() - 1)) {
                    ultimoCarac = true;
                    //NECESSÁRIO SOMAR, POIS O ÚLTIMO CARACTER
                    //NÃO TEM A COLUNA SÓ COM PIXELS BRANCOS APÓS ELE
                    //POR CONTA DISSO, É NECESSÁRIO PEGAR UMA COLUNA A MAIS
                    i++;
                }

            }
            //É NECESSÁRIO DUAS FLAGS PARA SABER SE A COLUNA POSSUI OU NÃO PIXEL PRETO
            //SE NÃO POSSUIR, SIGNIFICA QUE CHEGOU NO FINAL DO CARACTERER
            //ENTRETANTO, APÓS ENCONTRAR O PRIMEIRO CARACTERE, É PROVAVEL QUE TENHA
            //OUTRO ESPAÇO EM BRANCO, NÃO ENCONTRANDO O PIXEL PRETO NOVAMENTE,
            //SÓ QUE NESSA SITUAÇÃO ELE NÃO ENCONTROU O CARACTER
            //POR CONTA DISSO É NECESSÁRIO A FLAG encontrouCarac
            //PARA SABER SE JÁ APARECEU OUTRO CARACTER DEPOIS DA APARIÇÃO DO PRIMEIRO.
            //É PRECISO FAZER A VERFICICAÇÃO DO ÚLTIMO CARACTERE
            //POIS ELE NUNCA TERÁ UMA COLUNA EM BRANCO APÓS ELE
            if ((!encontrouPixelPreto && encontrouCarac) || ultimoCarac) {
                encontrouCarac = false;
                ImagePlus caracter = ij.IJ.createImage("ImagemJusta " + carac, "png", i - larguraCaracAnt, imgJusta.getHeight(), 8);
                ImageProcessor ipCaracter = caracter.getProcessor();
                //MONTA O CARACTER NA IMAGEM
                //COMEÇA DA LARGURA DO CARACTERE ANTERIOR E VAI ATÉ O i
                for (int j = larguraCaracAnt, largIni = 0; j < i; j++, largIni++) {
                    for (int k = 0; k < imgJusta.getHeight(); k++) {
                        ipCaracter.putPixel(largIni, k, ipJusta.getPixel(j, k));
                    }
                }

                IJ.save(caracter, dirImagensCortadas.getAbsolutePath() + "\\caracter" + carac + ".png");
                slicedImages.add(caracter);
                carac++;
                larguraCaracAnt = i;
                //SE FOR O PRIMEIRO CARACTER - SETA A BOOLEANA, POIS
                //SERÁ O PRIMEIRO CARACTER
                //SE NÃO FOR O PRIMEIRO CARACTER
                //POIS É NECESSÁRIO DESCONSIDERAR OS ESPAÇOS EM BRANCO
                //PARA DIVIDAR AS IMAGENS DOS CARACTERES DE FORMA JUSTA
                if (primeiroCarac) {
                    primeiroCarac = false;
                }
            }
            //SE NÃO É O PRIMEIRO CARACTER E NÃO ENCONTROU CARACTER, POIS SETOU
            //NO IF ACIMA E NÃO ENCONTROU O PIXEL PRETO
            //SIGNIFICA É HÁ ESPAÇO EM BRANCO PARA DESCONSIDERAR E SÓ ENTÃO
            //SERÁ POSSÍVEL DEIXAR A IMAGEM JUSTA COM O CARACTER
            if (!primeiroCarac && !encontrouCarac && !encontrouPixelPreto) {
                larguraCaracAnt++;
            }

            encontrouPixelPreto = false;

        }

        return slicedImages;
    }

    //MÉTODO QUE RECONHECE OS CARACTERES ATRAVÉS DA PASTA BaseImagens
    public String recognize(int limiar) {
        String palavra = "";
        
        File imagemBinaria = binarizaImagem(limiar);
        
        List<ImagePlus> slicedImages = imagensSeparadas(imagemJusta(imagemBinaria));

        File[] caracteres = dirBaseImagens.listFiles();

        //System.out.println("TAMANHO SLICED IMAGES: " + slicedImages.size());
        this.qtdCarac = slicedImages.size();
        for (ImagePlus si : slicedImages) {
            int contadorErros = 0;
            for (int i = 0; i < caracteres.length; i++) {
                boolean encontrouCaracter = true;
                File caractere = caracteres[i];
                //System.out.println("CARACTER: " + caractere.getAbsolutePath());
                ImagePlus imagemComparacao = ij.IJ.openImage(caractere.getAbsolutePath());
                ImageProcessor ipComparacao = imagemComparacao.getProcessor();

                ImageProcessor ipSliced = si.getProcessor();

                //COMPARA A LARGURA DAS IMAGENS
                //POIS A ALTURA SEMPRE SERÁ A MESMA PARA TODAS AS DIVISÕES
                //A LARGURA MUDARÁ POIS CADA CARACTER POSSUI UMA LARGURA ESPECÍFICA
                if (ipComparacao.getWidth() == ipSliced.getWidth()) {
                    for (int j = 0; j < ipComparacao.getWidth(); j++) {
                        for (int k = 0; k < ipComparacao.getHeight(); k++) {
                            int pixelComparacao = ipComparacao.getPixel(j, k);
                            int pixelSliced = ipSliced.getPixel(j, k);
                            
                            if (pixelComparacao != pixelSliced) {
                                encontrouCaracter = false;
                                contadorErros++;
                                break;
                            }

                        }
                        //SE TEM PIXEL DIFERENTE, SAIR DO LOOP
                        if (!encontrouCaracter) {
                            break;
                        }
                    }
                    //CONTA COMO UM ERRO QUANDO A IMAGEM NÃO TEM A MESMA LARGURA QUE A COMPARADA
                } else {
                    contadorErros++;
                    encontrouCaracter = false;
                }
                //SE ENCONTROU O CARACTER - CONCATENA NA PALAVRA E SAI DO LOOP
                if (encontrouCaracter) {
                    palavra = palavra + caractere.getName().replace(".png", "");
                    //System.out.println("Palavra: " + palavra);
                    break;
                }

            }

            if ((contadorErros == (caracteres.length - 1)) || caracteres.length == 0) {
                try {
                    System.out.println("NÃO IDENTIFICOU IMAGEM - VERIFICAR O DIRETORIO: " + dirBaseImagens.getAbsolutePath());
                    System.in.read();
                } catch (Exception e2) {

                }
            }
        }

        return palavra;
    }

}
