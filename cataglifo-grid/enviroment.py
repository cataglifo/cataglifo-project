class Ambiente:

    def __init__(self):
        self.mapa = MAPA

        self.inicio = INICIO

        self.objetivo = OBJETIVO

        self.linhas = len(self.mapa)

        self.colunas = len(self.mapa[0])

        self.posicao = self.inicio


    def reset(self):
        """
        Reinicia o ambiente.

        Faz o agente voltar para a posição inicial
        e retorna o estado inicial.
        """

        self.posicao = self.inicio

        return self.obter_estado()


    def obter_estado(self):
        """
        Retorna as informações que o agente conhece
        sobre a situação atual.

        Estado:
        [
            linha atual,
            coluna atual,
            linha do objetivo,
            coluna do objetivo
        ]
        """

        # self.posicao é uma tupla:
        # exemplo: (2, 3)
        linha, coluna = self.posicao

        # self.objetivo também é uma tupla:
        # exemplo: (4, 4)
        alvo_linha, alvo_coluna = self.objetivo

        # Retorna o estado em forma de lista
        return [
            linha,
            coluna,
            alvo_linha,
            alvo_coluna
        ]


    def posicao_valida(self, linha, coluna):
        """
        Verifica se uma posição pode ser ocupada pelo agente.

        Retorna:
        True  -> posição válida
        False -> posição inválida
        """

        # Verifica se saiu por cima ou por baixo do mapa
        if linha < 0 or linha >= self.linhas:
            return False

        # Verifica se saiu pela esquerda ou direita do mapa
        if coluna < 0 or coluna >= self.colunas:
            return False

        # No nosso mapa:
        # 0 = espaço livre
        # 1 = obstáculo
        #
        # Se for 1, o agente não pode entrar
        if self.mapa[linha][coluna] == 1:
            return False

        # Se passou por todos os testes,
        # então a posição é válida
        return True


    def step(self, acao):
        """
        Executa uma ação escolhida pelo agente.

        Retorna:

        (
            novo_estado,
            recompensa,
            terminou
        )

        Exemplo:
        (
            [1, 0, 4, 4],
            -0.1,
            False
        )
        """

        # Pega a posição atual do agente
        linha, coluna = self.posicao

        # Começamos assumindo que a nova posição
        # é igual à posição atual
        nova_linha = linha
        nova_coluna = coluna

        '''
        # Calcula a posição desejada
        '''

        if acao == CIMA:
            nova_linha -= 1

        elif acao == BAIXO:
            nova_linha += 1

        elif acao == ESQUERDA:
            nova_coluna -= 1

        elif acao == DIREITA:
            nova_coluna += 1

        '''
        Verifica se o movimento é válido
       '''

        if not self.posicao_valida(
            nova_linha,
            nova_coluna
        ):
            # Se a posição for inválida:
            #
            # - o agente NÃO se move
            # - recebe punição de -1
            # - o episódio continua

            return (
                self.obter_estado(),
                -1.0,
                False
            )

        '''
        
        # Movimento válido

        '''
        self.posicao = (
            nova_linha,
            nova_coluna
        )
        
        '''
        
         Verifica se chegou ao objetivo
       
        '''
        if self.posicao == self.objetivo:

            # Chegou ao objetivo:
            #
            # recompensa = +10
            # terminou = True

            return (
                self.obter_estado(),
                10.0,
                True
            )

        '''
        Movimento normal


         O agente conseguiu andar,
         mas ainda não chegou ao objetivo.
        
         Recebe uma pequena punição de -0.1.
        
         Isso incentiva o agente futuramente
         a encontrar caminhos mais curtos.
'''
        return (
            self.obter_estado(),
            -0.1,
            False
        )
