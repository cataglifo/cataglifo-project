class Ambiente:
    
    def __init__(self):
        self.mapa = MAPA
        self.inicio = INICIO
        self.objetivo = OBJETIVO
        
        self.linhas = len(self.mapa)
        self.colunas = len(self.mapa[0])
        
        self.posicao = self.inicio