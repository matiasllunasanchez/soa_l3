#include <Arduino.h>
#include <SoftwareSerial.h>
#include <stdio.h>
// ------------------------------------------------
// Etiquetas
// ------------------------------------------------
//#define LOG

// ------------------------------------------------
// Constantes
// ------------------------------------------------
#define INICIAL_INDICE 0
#define BAUDIOS 9600
#define RANGO_DIA_NOCHE 40
#define TMP_EVENTOS_MILI 500
#define NO 0
#define SI 1
#define ZERO 0
#define ONE 1
#define TEN 10
#define BASE 10
#define DY_BT 100
#define NCHARSBT 3
#define CR 13
#define LF 10
#define FL 35
#define BN '\n'
#define BZERO '\0'
#define FIFTY 50
#define RXPIN 8
#define TXPIN 12
#define OPCION_A 49
#define OPCION_B 50
#define OPCION_RED 51
#define OPCION_GREEN 52
#define OPCION_BLUE 53
#define OPCION_BLANCO 54
#define SOLICITUD_ENVIAR_ILU_DESEADA 57

// ------------------------------------------------
// Sensor LUZ INTERIOR
// ------------------------------------------------
#define PIN_LDR_INT A3

// ------------------------------------------------
// Sensor LUZ EXTERIOR
// ------------------------------------------------
#define PIN_LDR_EXT A2

#define SENSOR_VALOR_MINIMO 0
#define SENSOR_VALOR_MAXIMO 1023
#define SENSOR_VALOR_MINIMO_TABULADO 0
#define SENSOR_VALOR_MAXIMO_TABULADO 100

// ------------------------------------------------
// Actuador LED
// ------------------------------------------------
#define PIN_LED_R 9
#define PIN_LED_G 10
#define PIN_LED_B 11

#define VALOR_RED 250
#define VALOR_GREEN 180
#define VALOR_BLUE 165

#define POTENCIA_MAX_ILUMINACION 255
#define POTENCIA_MIN_ILUMINACION 0
#define VALOR_INCREMENTO 51

// ------------------------------------------------
// Pines motor roller
// ------------------------------------------------
#define PIN_E_MOTOR 5
#define PIN_MOTOR_A1 6
#define PIN_MOTOR_A2 7
#define TIEMPO_DE_MOVIMIENTO_DE_MOTOR 3000

// ------------------------------------------------
// Pines finales de carrera
// ------------------------------------------------
#define PIN_CARRERA_INF 2
#define PIN_CARRERA_SUP 3

// ------------------------------------------------
// Estados interruptor
// ------------------------------------------------
#define FINAL_CARRERA_PRESIONADO 1
#define FINAL_CARRERA_LIBRE 0

#define CANT_SENSORES 4

// ------------------------------------------------
// Estados del embebido
// ------------------------------------------------
enum estado_e
{
  ESTADO_ILUMINADOR_ESPERANDO,
  ESTADO_ILUMINADOR_MODO_DIA,
  ESTADO_ILUMINADOR_SUBIENDO_ROLLER,
  ESTADO_ILUMINADOR_BAJANDO_ROLLER,
  ESTADO_ILUMINADOR_MODO_NOCHE,
  ESTADO_ILUMINADOR_AJUSTADO
};

// ------------------------------------------------
// Eventos posibles
// ------------------------------------------------
enum evento_e
{
  EVENTO_ES_DIA,
  EVENTO_ES_NOCHE,
  EVENTO_ILUMINACION_AJUSTADA,
  EVENTO_FALTA_LUZ,
  EVENTO_SOBRA_LUZ,
  EVENTO_FINAL_SUP_PRESIONADO,
  EVENTO_FINAL_INF_PRESIONADO,
  EVENTO_CONTINUE
};

// ------------------------------------------------
// Estructura de evento
// ------------------------------------------------
typedef struct evento_s
{
  evento_e tipo;
  int valor;
} evento_t;

// ------------------------------------------------
// Estructura de sensor
// ------------------------------------------------
typedef struct sensor_luz_s
{
  int pin;
  int nivel;
} sensor_luz_t;

typedef struct sensor_final_carrera_s
{
  int pin;
  int nivel;
} sensor_final_carrera_t;

typedef struct actuador_led_s
{
  int pin_r;
  int pin_g;
  int pin_b;
} actuador_led_t;

typedef struct actuador_motor
{
  int pina;
  int pinb;
  int nivela;
  int nivelb;
  int enabled;
} actuador_motor_t;

// ------------------------------------------------
// Variables globales
// ------------------------------------------------
estado_e estado_actual;
evento_t evento;
sensor_luz_t sensor_ext;
sensor_luz_t sensor_int;
actuador_motor_t actuador_motor;
sensor_final_carrera_t final_carrera_sup;
sensor_final_carrera_t final_carrera_inf;
actuador_led_t actuador_led;

int potencia_iluminacion = ZERO;
unsigned long tiempo_anterior;
unsigned long tiempo_actual;
int indice;
int cortina_bajando;
int cortina_subiendo;
boolean sigo;
volatile int estado_final_sup;
volatile int estado_final_inf;
int max_iluminacion_deseada;
int min_iluminacion_deseada;
int iluminacion_deseada;
int dato_bt;
int ilu_recibida;
int user_red;
int user_green;
int user_blue;
SoftwareSerial bluetooth;

// ------------------------------------------------
// Funcion para escribir Logs
// ------------------------------------------------
void log(const char *estado, const char *evento)
{
#ifdef LOG
  Serial.println("------------------------------------------------");
  Serial.println(estado);
  Serial.println(evento);
  Serial.println("------------------------------------------------");
#endif
}

// ------------------------------------------------
// Logica de sensores
// ------------------------------------------------

int leer_sensor_luz(int pin)
{
  return map(analogRead(pin), SENSOR_VALOR_MINIMO, SENSOR_VALOR_MAXIMO, SENSOR_VALOR_MINIMO_TABULADO, SENSOR_VALOR_MAXIMO_TABULADO);
}

// ------------------------------------------------
// Verifico Iluminacion Exterior
// ------------------------------------------------
void verificar_dia_noche()
{
  sensor_ext.nivel = leer_sensor_luz(sensor_ext.pin);
  log("VERIFICANDO MODO", "");

  if (sensor_ext.nivel < RANGO_DIA_NOCHE)
  {
    evento.tipo = EVENTO_ES_NOCHE;
  }
  else
  {
    evento.tipo = EVENTO_ES_DIA;
  }
}

// ------------------------------------------------
// Verifico Iluminacion Interior
// ------------------------------------------------
void verificar_iluminacion_interna()
{
  sensor_int.nivel = leer_sensor_luz(sensor_int.pin);
  log("VERIFICANDO ILUMINACION INTERNA", "");
  if (sensor_int.nivel < min_iluminacion_deseada)
  {
    evento.tipo = EVENTO_FALTA_LUZ;
  }
  else if (sensor_int.nivel > max_iluminacion_deseada)
  {
    evento.tipo = EVENTO_SOBRA_LUZ;
  }
  else
  {
    evento.tipo = EVENTO_ILUMINACION_AJUSTADA;
  }
}

// ------------------------------------------------
// Verifico Finales de carrera
// ------------------------------------------------

void verificar_final_carrera_sup()
{
  log("VERIFICANDO FINAL DE CARRERA SUP", "");

  if (estado_final_sup == FINAL_CARRERA_PRESIONADO)
  {
    evento.tipo = EVENTO_FINAL_SUP_PRESIONADO;
  }
}

void verificar_final_carrera_inf()
{
  log("VERIFICANDO FINAL DE CARRERA INF", "");
  if (estado_final_inf == FINAL_CARRERA_PRESIONADO)
  {
    evento.tipo = EVENTO_FINAL_INF_PRESIONADO;
  }
}

// ------------------------------------------------
// Verificar Bluetooth
// ------------------------------------------------

void verificar_bluetooth()
{
  dato_bt = ZERO;
  if (bluetooth.available())
  {
    delay(DY_BT);
    dato_bt = bluetooth.read();
    Serial.println(dato_bt);
    if (dato_bt != CR && dato_bt != LF && dato_bt != FL)
    {
      evaluar_dato_bluetooth(dato_bt);
    }
  }
}

// ------------------------------------------------
// Evaluar Informacion recibida
// ------------------------------------------------

void evaluar_dato_bluetooth(int dato_bt)
{

  switch (dato_bt)
  {
  case OPCION_A:
    send_bluetooth(sensor_int.nivel);
    break;

  case OPCION_B:
    send_bluetooth(iluminacion_deseada);
    break;

  case OPCION_RED:
    led_red_bluetooth();
    break;

  case OPCION_GREEN:
    led_green_bluetooth();
    break;

  case OPCION_BLUE:
    led_blue_bluetooth();
    break;

  case OPCION_BLANCO:
    led_blanco_bluetooth();
    break;

  case SOLICITUD_ENVIAR_ILU_DESEADA:
    ilu_recibida = obtener_iluminacion_user();
    Serial.println(ilu_recibida);
    actualizar_iluminacion_user(ilu_recibida);

    break;
  default:
    break;
  }
}

// ------------------------------------------------
// Manejo de colores de la iluminacion
// ------------------------------------------------
void led_red_bluetooth()
{
  user_red = SI;
  user_green = NO;
  user_blue = NO;
  set_color(potencia_iluminacion, ZERO, ZERO);
}

void led_green_bluetooth()
{
  user_red = NO;
  user_green = SI;
  user_blue = NO;
  set_color(ZERO, potencia_iluminacion, ZERO);
}

void led_blue_bluetooth()
{
  user_red = NO;
  user_green = NO;
  user_blue = SI;
  set_color(ZERO, ZERO, potencia_iluminacion);
}

void led_blanco_bluetooth()
{
  user_red = NO;
  user_green = NO;
  user_blue = NO;
  set_color(potencia_iluminacion, potencia_iluminacion, potencia_iluminacion);
}

// ------------------------------------------------
// Setear Iluminacion deseada
// ------------------------------------------------

int obtener_iluminacion_user()
{

  static char message[NCHARSBT + ONE];
  static unsigned int message_pos = ZERO;

  while (bluetooth.available() > ZERO)
  {
    char inByte = bluetooth.read();

    if (inByte != BN && (message_pos < NCHARSBT) && inByte != FL)
    {

      message[message_pos] = inByte;
      message_pos++;
    }
    else
    {
      message[message_pos] = BZERO;
    }
  }
  message_pos = ZERO;
  int number = atoi(message);
  return number;
}

void actualizar_iluminacion_user(int ilu)
{
  iluminacion_deseada = ilu;
  min_iluminacion_deseada = iluminacion_deseada - TEN;
  max_iluminacion_deseada = iluminacion_deseada + TEN;
}

// ------------------------------------------------
// Enviar Entero a Android
// ------------------------------------------------
void send_bluetooth(int valor)
{
  char cstr[NCHARSBT - ONE];

  itoa(valor, cstr, BASE);
  for (int n = ZERO; n < NCHARSBT - ONE; n++)
  {
    bluetooth.print(cstr[n]);
    delay(DY_BT);
  }
  bluetooth.println("#");
}
// ------------------------------------------------
// Logica de actuadores
// ------------------------------------------------

// ------------------------------------------------
// Cotrol de Roller
// ------------------------------------------------

void parar_roller()
{
  log("FINAL CARRERA ACTIVO ", "INTERRUPCION");
  digitalWrite(actuador_motor.pina, LOW);
  digitalWrite(actuador_motor.pinb, LOW);
  if (cortina_subiendo)
  {
    estado_final_sup = FINAL_CARRERA_PRESIONADO;
    cortina_subiendo = NO;
  }
  if (cortina_bajando)
  {
    estado_final_inf = FINAL_CARRERA_PRESIONADO;
    cortina_bajando = NO;
  }
}

void set_final_carrera_sup()
{
  if (estado_final_sup == FINAL_CARRERA_PRESIONADO)
  {
    estado_final_sup = FINAL_CARRERA_LIBRE;
  }
}

void set_final_carrera_inf()
{
  if (estado_final_inf == FINAL_CARRERA_PRESIONADO)
  {
    estado_final_inf = FINAL_CARRERA_LIBRE;
  }
}

void fn_activar_giro_up()
{
  if (estado_final_sup == FINAL_CARRERA_LIBRE)
  {

    log("ACTIVANDO SUBIDA", "");
    digitalWrite(actuador_motor.pina, LOW);
    digitalWrite(actuador_motor.pinb, HIGH);
    cortina_subiendo = SI;
    set_final_carrera_inf();
  }
  else
  {
    estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
  }
}

void fn_activar_giro_down()
{
  if (estado_final_inf == FINAL_CARRERA_LIBRE)
  {

    log("ACTIVANDO BAJADA", "");
    digitalWrite(actuador_motor.pina, HIGH);
    digitalWrite(actuador_motor.pinb, LOW);
    cortina_bajando = SI;
    set_final_carrera_sup();
  }
  else
  {
    estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
  }
}

void fn_desactivar_giro_up()
{
  log("DESACTIVANDO MOVIMIENTO UP", "");
  digitalWrite(actuador_motor.pina, LOW);
  digitalWrite(actuador_motor.pinb, LOW);
  cortina_subiendo = NO;
}

void fn_desactivar_giro_down()
{
  log("DESACTIVANDO MOVIMIENTO DOWN", "");
  digitalWrite(actuador_motor.pina, LOW);
  digitalWrite(actuador_motor.pinb, LOW);
  cortina_bajando = NO;
}

void fn_subir_roller()
{
  if (!cortina_subiendo)
  {
    fn_activar_giro_up();
  }
}

void fn_bajar_roller()
{

  if (!cortina_bajando)
  {

    fn_activar_giro_down();
  }
}

// ------------------------------------------------
// LED RGB
// ------------------------------------------------

void set_color(int red, int green, int blue)
{

  if (user_red == SI)
  {
    analogWrite(actuador_led.pin_r, red);
    analogWrite(actuador_led.pin_g, ZERO);
    analogWrite(actuador_led.pin_b, ZERO);
  }
  else if (user_green == SI)
  {
    analogWrite(actuador_led.pin_r, ZERO);
    analogWrite(actuador_led.pin_g, green);
    analogWrite(actuador_led.pin_b, ZERO);
  }
  else if (user_blue == SI)
  {
    analogWrite(actuador_led.pin_r, ZERO);
    analogWrite(actuador_led.pin_g, ZERO);
    analogWrite(actuador_led.pin_b, blue);
  }
  else
  {
    analogWrite(actuador_led.pin_r, red);
    analogWrite(actuador_led.pin_g, green);
    analogWrite(actuador_led.pin_b, blue);
  }
}

void apagar_led()
{
  potencia_iluminacion = 0;
  digitalWrite(actuador_led.pin_r, LOW);
  digitalWrite(actuador_led.pin_g, LOW);
  digitalWrite(actuador_led.pin_b, LOW);
  log("APAGANDO ILUMINACION", "");
}

void fn_aumentar_intensidad_led()
{

  if (potencia_iluminacion < POTENCIA_MAX_ILUMINACION)
  {

    potencia_iluminacion += VALOR_INCREMENTO;
    set_color(potencia_iluminacion, potencia_iluminacion, potencia_iluminacion);
    estado_actual = ESTADO_ILUMINADOR_MODO_NOCHE;
  }
  else
  {
    estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
  }
  log("AUMENTANDO ILUMINACION", "");
}
void fn_disminuye_intensidad_led()
{
  if (potencia_iluminacion > POTENCIA_MIN_ILUMINACION)
  {

    potencia_iluminacion -= VALOR_INCREMENTO;
    set_color(potencia_iluminacion, potencia_iluminacion, potencia_iluminacion);
    estado_actual = ESTADO_ILUMINADOR_MODO_NOCHE;
  }
  else
  {
    estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
  }
  log("DISMINUYENDO ILUMINACION", "");
}

// ------------------------------------------------
// Captura de eventos
// ------------------------------------------------
void (*verificar_sensor[CANT_SENSORES])() = {verificar_dia_noche, verificar_iluminacion_interna, verificar_final_carrera_sup, verificar_final_carrera_inf};

void tomar_evento()
{

  tiempo_actual = millis();
  if ((tiempo_actual - tiempo_anterior) > TMP_EVENTOS_MILI)
  {
    log("TOMANDO EVENTO", "");
    verificar_sensor[indice]();
    indice = ++indice % CANT_SENSORES;
    tiempo_anterior = tiempo_actual;

    verificar_bluetooth();
  }
  else
  {
    evento.tipo = EVENTO_CONTINUE;
  }
}

// ------------------------------------------------
// Inicialización
// ------------------------------------------------
void carga_inicio()
{
  potencia_iluminacion = ZERO;
  indice = INICIAL_INDICE;
  cortina_bajando = ZERO;
  cortina_subiendo = ZERO;
  sigo = true;
  estado_final_sup = FINAL_CARRERA_LIBRE;
  estado_final_inf = FINAL_CARRERA_LIBRE;
  max_iluminacion_deseada = ZERO;
  min_iluminacion_deseada = ZERO;
  iluminacion_deseada = FIFTY;
  dato_bt = ZERO;
  ilu_recibida = ZERO;
  user_red = ZERO;
  user_green = ZERO;
  user_blue = ZERO;
  Serial.begin(BAUDIOS);
  bluetooth = SoftwareSerial(RXPIN, TXPIN);
  bluetooth.begin(BAUDIOS);
  pinMode(RXPIN, INPUT);
  pinMode(TXPIN, OUTPUT);
  sensor_ext.pin = PIN_LDR_EXT;
  pinMode(sensor_ext.pin, INPUT);
  sensor_int.pin = PIN_LDR_INT;
  pinMode(sensor_int.pin, INPUT);
  final_carrera_sup.pin = PIN_CARRERA_SUP;
  pinMode(final_carrera_sup.pin, INPUT_PULLUP);
  final_carrera_inf.pin = PIN_CARRERA_INF;
  pinMode(final_carrera_inf.pin, INPUT_PULLUP);
  actuador_led.pin_r = PIN_LED_R;
  pinMode(actuador_led.pin_r, OUTPUT);
  actuador_led.pin_g = PIN_LED_G;
  pinMode(actuador_led.pin_g, OUTPUT);
  actuador_led.pin_b = PIN_LED_B;
  pinMode(actuador_led.pin_b, OUTPUT);
  actuador_motor.enabled = PIN_E_MOTOR;
  pinMode(actuador_motor.enabled, OUTPUT);
  digitalWrite(actuador_motor.enabled, HIGH);
  actuador_motor.pina = PIN_MOTOR_A1;
  pinMode(actuador_motor.pina, OUTPUT);
  actuador_motor.pinb = PIN_MOTOR_A2;
  pinMode(actuador_motor.pinb, OUTPUT);
  attachInterrupt(digitalPinToInterrupt(final_carrera_sup.pin), parar_roller, RISING);
  attachInterrupt(digitalPinToInterrupt(final_carrera_inf.pin), parar_roller, RISING);
  estado_actual = ESTADO_ILUMINADOR_ESPERANDO;
  cortina_bajando = NO;
  cortina_subiendo = NO;
  min_iluminacion_deseada = iluminacion_deseada - TEN;
  max_iluminacion_deseada = iluminacion_deseada + TEN;
}

// ------------------------------------------------
// Maquina de estados
// ------------------------------------------------
void maquina_de_estados()
{
  tomar_evento();

  switch (estado_actual)
  {
  case ESTADO_ILUMINADOR_ESPERANDO:
    switch (evento.tipo)
    {
    case EVENTO_ES_DIA:
      estado_actual = ESTADO_ILUMINADOR_MODO_DIA;
      apagar_led();
      log("ESTADO_ILUMINADOR_ESPERANDO", "EVENTO_ES_DIA");
      break;

    case EVENTO_ES_NOCHE:
      estado_actual = ESTADO_ILUMINADOR_MODO_NOCHE;
      log("ESTADO_ILUMINADOR_ESPERANDO", "EVENTO_ES_NOCHE");
      break;

    case EVENTO_CONTINUE:
      log("ESTADO_ILUMINADOR_ESPERANDO", "EVENTO_CONTINUE");
      estado_actual = ESTADO_ILUMINADOR_ESPERANDO;
      break;

    default:
      estado_actual = ESTADO_ILUMINADOR_ESPERANDO;
      break;
    }
    break;

  case ESTADO_ILUMINADOR_MODO_DIA:
    switch (evento.tipo)
    {
    case EVENTO_FALTA_LUZ:
      fn_subir_roller();
      estado_actual = ESTADO_ILUMINADOR_SUBIENDO_ROLLER;
      log("ESTADO_ILUMINADOR_MODO_DIA", "EVENTO_FALTA_LUZ");
      break;

    case EVENTO_SOBRA_LUZ:
      fn_bajar_roller();
      estado_actual = ESTADO_ILUMINADOR_BAJANDO_ROLLER;
      log("ESTADO_ILUMINADOR_MODO_DIA", "EVENTO_SOBRA_LUZ");
      break;

    case EVENTO_CONTINUE:
      estado_actual = ESTADO_ILUMINADOR_MODO_DIA;
      log("ESTADO_ILUMINADOR_MODO_DIA", "EVENTO_CONTINUE");
      break;

    default:
      estado_actual = ESTADO_ILUMINADOR_MODO_DIA;
      break;
    }
    break;

  case ESTADO_ILUMINADOR_SUBIENDO_ROLLER:
    switch (evento.tipo)
    {
    case EVENTO_CONTINUE:
      estado_actual = ESTADO_ILUMINADOR_SUBIENDO_ROLLER;
      log("ESTADO_ILUMINADOR_SUBIENDO_ROLLER", "EVENTO_CONTINUE");
      break;

    case EVENTO_ILUMINACION_AJUSTADA:
      fn_desactivar_giro_up();
      estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
      log("ESTADO_ILUMINADOR_SUBIENDO_ROLLER", "EVENTO_ILUMINACION_AJUSTADA");
      break;

    case EVENTO_FINAL_SUP_PRESIONADO:

      estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
      log("ESTADO_ILUMINADOR_SUBIENDO_ROLLER", "EVENTO_FINAL_SUP_PRESIONADO");
      break;

    default:
      estado_actual = ESTADO_ILUMINADOR_SUBIENDO_ROLLER;
      break;
    }
    break;

  case ESTADO_ILUMINADOR_BAJANDO_ROLLER:
    switch (evento.tipo)
    {

    case EVENTO_CONTINUE:
      estado_actual = ESTADO_ILUMINADOR_BAJANDO_ROLLER;
      log("ESTADO_ILUMINADOR_BAJANDO_ROLLER", "EVENTO_CONTINUE");
      break;

    case EVENTO_ILUMINACION_AJUSTADA:
      fn_desactivar_giro_down();
      estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
      log("ESTADO_ILUMINADOR_BAJANDO_ROLLER", "EVENTO_ILUMINACION_AJUSTADA");
      break;

    case EVENTO_FINAL_INF_PRESIONADO:

      estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
      log("ESTADO_ILUMINADOR_BAJANDO_ROLLER", "EVENTO_FINAL_INF_PRESIONADO");
      break;

    default:
      estado_actual = ESTADO_ILUMINADOR_BAJANDO_ROLLER;
      break;
    }
    break;

  case ESTADO_ILUMINADOR_MODO_NOCHE:
    switch (evento.tipo)
    {
    case EVENTO_FALTA_LUZ:
      fn_aumentar_intensidad_led();
      log("ESTADO_ILUMINADOR_MODO_NOCHE", "EVENTO_FALTA_LUZ");
      break;

    case EVENTO_SOBRA_LUZ:
      fn_disminuye_intensidad_led();
      log("ESTADO_ILUMINADOR_MODO_NOCHE", "EVENTO_SOBRA_LUZ");
      break;

    case EVENTO_CONTINUE:
      estado_actual = ESTADO_ILUMINADOR_MODO_NOCHE;
      log("ESTADO_ILUMINADOR_MODO_NOCHE", "EVENTO_CONTINUE");
      break;

    case EVENTO_ILUMINACION_AJUSTADA:
      estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
      log("ESTADO_ILUMINADOR_MODO_NOCHE", "EVENTO_ILUMINACION_AJUSTADA");
      break;

    default:
      estado_actual = ESTADO_ILUMINADOR_MODO_NOCHE;
      break;
    }
    break;

  case ESTADO_ILUMINADOR_AJUSTADO:
    switch (evento.tipo)
    {

    case EVENTO_FALTA_LUZ:
      estado_actual = ESTADO_ILUMINADOR_ESPERANDO;
      log("ESTADO_ILUMINADOR_AJUSTADO", "EVENTO_FALTA_LUZ");
      break;

    case EVENTO_SOBRA_LUZ:
      estado_actual = ESTADO_ILUMINADOR_ESPERANDO;
      log("ESTADO_ILUMINADOR_AJUSTADO", "EVENTO_SOBRA_LUZ");
      break;

    case EVENTO_CONTINUE:
      estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
      log("ESTADO_ILUMINADOR_AJUSTADO", "EVENTO_CONTINUE");
      break;

    default:
      estado_actual = ESTADO_ILUMINADOR_AJUSTADO;
      break;
    }
    break;
  }
}

void setup()
{
  carga_inicio();
}

void loop()
{
  maquina_de_estados();
}