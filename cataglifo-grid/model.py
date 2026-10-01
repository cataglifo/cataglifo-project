import torch.nn as nn

class RedeQ(nn.Module):
    def __init__(self):
        super().__init__()
        self.rede = nn.Sequential(
            
            nn.Linear(4, 32), ## 4 valores, 32 neurônios (mais que o vianna)
            
            nn.ReLU(), # permite que a rede aprenda funções não lineares.
            
            nn.Linear(32, 32),
            
            nn.ReLU(),
            
            nn.Linear(32, 4) #32 valores, 4 respostas (4 ações possíveis)
            
        )
        
    def forward(self, estado):
        
        return self.rede(estado)
    
    
