#!/bin/bash
prog=[]
program=[]
count=0
for var in "$@"
do      
    program[count]=${var%.*}
    let "count++"
done
arraylength=${#program[@]}

export ASM_HOME="./lib"
rm -rf .branches
rm -rf .temp
javac -cp .:$ASM_HOME/asm-6.0.jar:$ASM_HOME/asm-commons-6.0.jar Instrumenter.java
for (( i=0; i<${arraylength}; i++ ));
do
    java -cp .:$ASM_HOME/asm-6.0.jar:$ASM_HOME/asm-commons-6.0.jar Instrumenter ${program[i]}.class ${program[i]}.class
done


