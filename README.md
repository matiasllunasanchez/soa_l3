# soa_l3
# Proyecto de sistemas operativos avanzados - ENTREGA FINAL

### Introducción:
Esta aplicacion tiene como objetivo conectarse el dispositivo (arduino) por bluetooth para las ejecutar diferentes funcionalidades.
El dispositivo es una cortina inteligente y la aplicación contará con funcionalidades tales como configurar color del led o configurar la luminosidad deseada para la habitacion, y/o consultar la luminosidad actual de la misma.

## Descripcion general
#### Activity 1:
Es la pantalla referida al seteo de la iluminacion deseada en la habitación. 
Consta de un texto que informa la iluminacion actual captada por el dispositivo en la habitación. 
Debajo se puede observar un campo input para poder indicar la iluminación deseada a configurar en la habitación mediante el dispositivo. 
Al entrar la primera vez a la pantalla se realiza una consulta sobre la luminosidad deseada que se encuentra configurada en el dispositivo la cual se precarga. 
Luego se permite al usuario cambiarla si lo desea.
Los valores son del 0 al 100 referidos al porcentaje de luminosidad. Tambien se cuenta con un seekbar para poder indicar el valor utilizando el control amigable con el usuario y no un campo de texto. 
Al presionar guardar, el valor que se encuentre en el input se enviará por BT al dispositivo.

#### Activity 2: 
Es la pantalla referida al cambio de color de led y la utilización del sensor acerelometro del dispositivo para detectar un efecto "shake" y cambiar aleatoriamente el color el cual se envia hacia el dispositivo por BT.
Los colores estan limitados en ROJO, BLANCO, VERDE y AZUL.


Todas las pantallas cuentan con una imagen descriptiva de la acción realizada representada por una lamparita, siendo esta intensidad luminica representada por los rayitos alrededor de la lampara y en la segunda pantalla el color del led afectando literalmente a la imagen de la lampara.
