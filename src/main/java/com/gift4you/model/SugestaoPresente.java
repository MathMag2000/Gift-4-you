package com.gift4you.model;

import java.util.List;

public record SugestaoPresente(Presente presente, int pontuacao, List<String> motivos) {
}
