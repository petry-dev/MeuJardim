# Meu Jardim

Aplicativo Android para cuidar das plantas da casa, com lembretes de rega que funcionam mesmo com o app fechado.

## Como abrir e rodar

1. Abra a pasta `MeuJardim` no Android Studio (Ladybug 2024.2 ou mais recente).
2. Aguarde o Gradle sincronizar. O Android Studio cria o `local.properties` com o caminho do SDK.
3. Rode no emulador ou no aparelho (Android 8.0 / API 26 ou superior).

Pela linha de comando: `./gradlew assembleDebug` (APK em `app/build/outputs/apk/debug/`).

Requisitos: JDK 17, Android SDK 35.

## Stack

| Camada | Tecnologia |
|---|---|
| Interface | Kotlin, Jetpack Compose, Material 3 |
| Navegação | Navigation Compose |
| Dados | Room (plantas e registros de rega) |
| Ajustes | DataStore Preferences |
| Lembretes | AlarmManager (alarmes exatos) + BroadcastReceivers |
| Fotos | Photo Picker, câmera via FileProvider, Coil |
| Clima | Open-Meteo (gratuita, sem chave de API) |

A injeção de dependências é manual (`AppContainer` em `MeuJardimApp.kt`) para manter o projeto enxuto. Dá para migrar para Hilt depois sem mudar as camadas.

## Estrutura

```
app/src/main/java/br/com/meujardim/
├── MeuJardimApp.kt          Application + AppContainer
├── MainActivity.kt          Entrada; trata "Ver planta" vindo da notificação
├── data/
│   ├── db/                  Room: Plant, WateringLog, DAOs, conversores
│   ├── prefs/               DataStore: nome, horários, adiamento, cidade
│   ├── weather/             Open-Meteo: previsão, busca de cidade, recomendações
│   ├── catalog/             Catálogo de 35 espécies com sugestões de rega
│   └── GardenRepository.kt  Regras de negócio (regar, adiar, corrigir registro)
├── notifications/
│   ├── AlarmScheduler.kt    Agenda os horários e os adiamentos
│   ├── NotificationHelper.kt Canais, textos e ações rápidas
│   └── Receivers.kt         Disparo dos lembretes, ações e reboot
├── ui/
│   ├── dashboard/           Tela inicial
│   ├── plants/              Lista, cadastro/edição e detalhe
│   ├── history/             Calendário e correção de registros
│   ├── settings/            Ajustes
│   ├── components/          Componentes visuais compartilhados
│   ├── navigation/          NavHost e barra inferior
│   └── theme/               Cores, tipografia e formas
└── util/                    Datas em pt-BR, fotos
```

## Como funcionam os lembretes

- Existe **um alarme por horário** (09h, 14h e 19h por padrão), e não um alarme por planta. Quando o alarme dispara, o app consulta o banco e notifica **cada planta com rega pendente**, numa notificação separada para cada uma.
- O 1º horário envia "Hora de regar", o intermediário envia "ainda está na lista de hoje" e o último envia "Último lembrete de hoje".
- Se a rega não for confirmada, a planta continua pendente. No dia seguinte, ela passa a receber o aviso de **rega atrasada**, que orienta a verificar a umidade do solo e as necessidades da espécie. O app nunca afirma que a planta está enfraquecendo.
- **Já reguei!** registra a data e o horário, recalcula a próxima rega, cancela os lembretes restantes e atualiza o histórico, tudo sem abrir o app.
- **Lembrar mais tarde** cria um alarme só para aquela planta, com o intervalo definido em Ajustes (15 min a 3 h). Enquanto o adiamento vale, os horários fixos ignoram essa planta.
- **Ver planta** abre o detalhe da planta.
- O app nunca marca uma planta como regada automaticamente.
- Os alarmes são recriados após reiniciar o aparelho, atualizar o app ou mudar a data, a hora ou o fuso.

### Permissões

- **Notificações** (Android 13+): pedida na primeira abertura. Se for negada, aparece um cartão de aviso no início e nos ajustes.
- **Alarmes exatos** (Android 12+): no Android 14 ela vem desativada por padrão para apps novos. Sem ela, o app usa alarmes inexatos, que podem atrasar alguns minutos, e mostra um cartão com atalho para liberar.
- Em aparelhos com economia de bateria agressiva (Xiaomi, Samsung, Motorola), vale tirar o Meu Jardim da otimização de bateria.

Em **Ajustes > Testar notificações** há um botão que envia um aviso de exemplo com as três ações.

## Regras de cálculo

- Próxima rega = data da última rega + frequência. Sem última rega informada, a próxima rega é hoje.
- A próxima data pode ser escolhida manualmente no formulário. O botão "Recalcular" volta ao cálculo automático.
- Ao corrigir ou excluir um registro no histórico, a última e a próxima rega são recalculadas a partir do registro mais recente.
- Um registro conta como atrasado quando a rega aconteceu depois da data prevista naquele momento.

## Sugestão automática por espécie

`SpeciesCatalog.kt` tem 35 espécies comuns no Brasil (jiboia, espada-de-são-jorge, costela-de-adão, suculentas, temperos etc.), cada uma com frequência, quantidade de água, luz e uma dica. A busca ignora acentos e aceita nomes populares, científicos e apelidos. Se o usuário alterar a frequência ou a quantidade à mão, a sugestão automática é desligada. Espécies fora do catálogo começam com 7 dias e 250 ml.

Os valores do catálogo são pontos de partida para vasos médios em ambiente interno. Revise-os com uma fonte especializada antes de publicar o app.

## Clima

O resumo do clima mostra temperatura atual, máxima e mínima, chance de chuva e umidade da cidade definida em Ajustes. As recomendações são apenas sugestões, como em dias quentes, ar seco, noites frias e chuva para plantas na varanda ou no jardim, e nunca alteram a agenda de regas.

## Próximos passos sugeridos

- Backup e sincronização (Firebase ou um backend próprio).
- Adubação, poda e troca de vaso como novos tipos de tarefa (o modelo `WateringLog` pode virar `CareLog` com um campo de tipo).
- Widget da tela inicial com as regas do dia.
- Identificação de espécie por foto.
- Testes instrumentados do `GardenRepository` e do fluxo de notificações.
