#!/bin/bash
java -cp ~/.m2/repository/com/h2database/h2/2.4.240/h2-2.4.240.jar org.h2.tools.Console -url "jdbc:h2:$(pwd)/database" -user sa -password ""
