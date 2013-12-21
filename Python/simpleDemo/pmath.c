#include <assert.h>
#include <stdio.h>
int mul(int num1, int num2)
{
  return num1 * num2;
}

int add(int num1, int num2)
{
  return num1 + num2;
}

int sub(int num1, int num2)
{
  return num1 - num2;
}

int div(int num1, int num2)
{
  assert(num2 != 0);
  return num1 / num2;
}

int mod(int num1, int num2)
{
  assert(num2 != 0);
  return num1 % num2;
}

int main(int argc, char** args)
{
  printf("mul:%d\n",mul(2,3));
  printf("add:%d\n",add(2,3));
  printf("sub:%d\n",sub(2,3));
  printf("div:%d\n",div(2,3));
  printf("mod:%d\n",mod(2,3));
}
